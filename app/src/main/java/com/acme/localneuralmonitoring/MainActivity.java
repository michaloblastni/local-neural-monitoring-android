// MainActivity.java

package com.acme.localneuralmonitoring;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.acme.localneuralmonitoring.USB_PERMISSION";
    private static final int MAX_POINTS = 512;
    private static final int SAMPLE_RATE = 256;

    private UsbSerialPort serialPort;
    private TextView statusText;
    private Spinner bandSpinner;
    private EEGView eegPlot;
    private Button recordButton;

    private float[][] data = new float[MAX_POINTS][2];
    private int dataIndex = 0;
    private boolean recording = false;
    private FileWriter recordFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link GUI components
        statusText = findViewById(R.id.statusText);
        bandSpinner = findViewById(R.id.bandSpinner);
        eegPlot = findViewById(R.id.eegPlot);

        // Redraw the latest EEG data on startup or after rotation
        float[][] copy = new float[MAX_POINTS][2];
        int base = (dataIndex + MAX_POINTS - 1) % MAX_POINTS;
        for (int j = 0; j < MAX_POINTS; j++) {
            int idx = (base + j) % MAX_POINTS;
            copy[j][0] = data[idx][0];
            copy[j][1] = data[idx][1];
        }

        eegPlot.setWaveform(copy);

        recordButton = findViewById(R.id.recordButton);
        recordButton.bringToFront();


        recordButton.setOnClickListener(v -> {
            recording = !recording;
            if (recording) {
                try {
                    recordFile = new FileWriter(getExternalFilesDir(null) + "/eeg_data.csv");
                    recordFile.write("Counter,Ch1,Ch2\n");
                    statusText.setText("Recording started");
                    recordButton.setText("Stop Recording");
                } catch (IOException e) {
                    statusText.setText("Error: " + e.getMessage());
                    recording = false;
                }
            } else {
                try {
                    if (recordFile != null) recordFile.close();
                } catch (IOException ignored) {}
                statusText.setText("Recording stopped");
                recordButton.setText("Start Recording");
            }
        });

        openSerialAndStartReading();
    }

    private void openSerialAndStartReading() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        if (availableDrivers.isEmpty()) {
            statusText.setText("No serial device found");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());

        if (connection == null) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(driver.getDevice(), permissionIntent);
            statusText.setText("Permission requested. Reconnect device.");
            return;
        }

        serialPort = driver.getPorts().get(0);
        try {
            serialPort.open(connection);
            serialPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            statusText.setText("Serial error: " + e.getMessage());
            return;
        }

        startReadingLoop();
    }

    private void startReadingLoop() {
        new Thread(() -> {
            final int PACKET_SIZE = 17;
            final byte HEADER1 = (byte) 0xA5;
            final byte HEADER2 = (byte) 0x5A;

            byte[] buffer = new byte[1024];  // temporary receive buffer
            int bufferLen = 0;

            byte[] readBuf = new byte[64];

            try {
                while (true) {
                    int len = serialPort.read(readBuf, 100);
                    if (len <= 0) continue;

                    // Append new data to our buffer
                    if (bufferLen + len > buffer.length) {
                        // Shift old data if buffer overflows
                        System.arraycopy(buffer, bufferLen - 256, buffer, 0, 256);
                        bufferLen = 256;
                    }
                    System.arraycopy(readBuf, 0, buffer, bufferLen, len);
                    bufferLen += len;

                    // Search for full packets
                    int ptr = 0;
                    while (ptr <= bufferLen - PACKET_SIZE) {
                        if ((buffer[ptr] & 0xFF) == 0xA5 && (buffer[ptr + 1] & 0xFF) == 0x5A) {
                            byte[] packet = new byte[PACKET_SIZE];
                            System.arraycopy(buffer, ptr, packet, 0, PACKET_SIZE);

                            int counter = packet[3] & 0xFF;
                            int ch1 = ((packet[4] & 0xFF) << 8) | (packet[5] & 0xFF);
                            int ch2 = ((packet[6] & 0xFF) << 8) | (packet[7] & 0xFF);

                            data[dataIndex][0] = ch1;
                            data[dataIndex][1] = ch2;
                            // Log.d("EEG", "CH1: " + ch1 + ", CH2: " + ch2 + ", Counter: " + counter);
                            dataIndex = (dataIndex + 1) % MAX_POINTS;

                            if (recording && recordFile != null) {
                                recordFile.write(counter + "," + ch1 + "," + ch2 + "\n");
                                recordFile.flush();
                            }

                            float[][] copy = new float[MAX_POINTS][2];
                            int base = (dataIndex + MAX_POINTS - 1) % MAX_POINTS;
                            for (int j = 0; j < MAX_POINTS; j++) {
                                int idx = (base + j) % MAX_POINTS;
                                copy[j][0] = data[idx][0];
                                copy[j][1] = data[idx][1];
                            }

                            eegPlot.setWaveform(copy);
                            ptr += PACKET_SIZE;
                        } else {
                            ptr++;
                        }
                    }

                    // Shift remaining data to beginning of buffer
                    int remaining = bufferLen - ptr;
                    System.arraycopy(buffer, ptr, buffer, 0, remaining);
                    bufferLen = remaining;
                }
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Read error: " + e.getMessage()));
            }
        }).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        eegPlot.invalidate(); // force a redraw
    }
}
