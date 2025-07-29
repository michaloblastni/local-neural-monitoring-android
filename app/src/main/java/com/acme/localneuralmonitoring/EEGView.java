package com.acme.localneuralmonitoring;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class EEGView extends SurfaceView implements SurfaceHolder.Callback {
    private float[][] waveform = new float[512][2];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isSurfaceReady = false;

    public EEGView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setZOrderOnTop(true);
        setBackgroundColor(Color.YELLOW);
    }

    public void setWaveform(float[][] data) {
        if (data != null && data.length > 1) {
            this.waveform = data;
            if (isSurfaceReady) {
                post(this::drawWaveform);
            }
        }
    }

    private void drawWaveform() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;

            canvas.drawColor(Color.WHITE);
            int width = canvas.getWidth();
            int height = canvas.getHeight();

            int leftMargin = 60;
            int plotWidth = width - leftMargin;
            int channelPadding = 10;
            int channelHeight = (height - channelPadding) / 2;

            float displayRange = 1024f;
            int[] colors = { Color.rgb(0, 128, 255), Color.rgb(255, 0, 0) };
            String[] labels = { "CH1", "CH2" };

            for (int ch = 0; ch < 2; ch++) {
                int yBase = ch * (channelHeight + channelPadding);

                // Draw horizontal grid lines
                paint.setColor(Color.LTGRAY);
                for (int i = 0; i <= 4; i++) {
                    float value = displayRange - (i * (displayRange / 4.0f)); // 1024 → 0
                    float norm = (value / displayRange);
                    int y = yBase + (int)(norm * channelHeight);
                    canvas.drawLine(leftMargin, y, width, y, paint);

                    // Labels: 0 (top) to 1024 (bottom)
                    paint.setColor(Color.DKGRAY);
                    paint.setTextSize(24);
                    canvas.drawText(String.format("%.0f", displayRange - value), 5, y + 8, paint);
                    paint.setColor(Color.LTGRAY);
                }

                // Plot waveform
                paint.setColor(colors[ch]);
                paint.setStrokeWidth(2f);
                for (int i = 0; i < waveform.length - 1; i++) {
                    float v1 = clamp(waveform[i][ch], 0, displayRange);
                    float v2 = clamp(waveform[i + 1][ch], 0, displayRange);

                    float norm1 = v1 / displayRange;
                    float norm2 = v2 / displayRange;

                    float x1 = leftMargin + (i * plotWidth) / (float) waveform.length;
                    float x2 = leftMargin + ((i + 1) * plotWidth) / (float) waveform.length;

                    float y1 = yBase + norm1 * channelHeight;
                    float y2 = yBase + norm2 * channelHeight;

                    canvas.drawLine(x1, y1, x2, y2, paint);
                }

                // Draw channel label
                paint.setColor(colors[ch]);
                canvas.drawText(labels[ch], width - 50, yBase + 30, paint);

                // Separator between channels
                if (ch == 0) {
                    paint.setColor(Color.GRAY);
                    int separatorY = yBase + channelHeight + (channelPadding / 2);
                    canvas.drawLine(leftMargin, separatorY, width, separatorY, paint);
                }
            }

        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceReady = true;
        drawWaveform();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceReady = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        isSurfaceReady = true;  // Ensure we allow drawWaveform
        post(this::drawWaveform);  // Force draw using latest waveform
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        return false; // Don't consume any touch events
    }
}
