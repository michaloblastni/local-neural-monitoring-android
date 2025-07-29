# Local Neural Monitoring for EEG-SMT (Android) 0.0.1

This Android app lets you connect an **EEG-SMT** device to your smartphone using a USB OTG adapter and visualize live EEG data in real time.

---

## 📱 APK Installation

**➡️ [Download APK (v0.0.1, unsigned)](https://github.com/michaloblastni/local-neural-monitoring-android/releases/download/0.0.1/localneuralmonitoring0.0.1.apk)**

> ⚠️ This APK is **unsigned**. Installation requires enabling "Unknown sources" on your Android device.

---

## 🧠 Supported Hardware

- **EEG-SMT** by Olimex  
- USB connection (CDC Virtual COM over UART)  
- Sampling rate: 256 Hz  
- Requires Android device with USB Host support (Android 8.0+ recommended)

---

## 🔌 What You Need
- EEG-SMT device with USB cable  
- A **USB OTG adapter or hub**  
  - Example: [USB-C OTG Adapter](https://www.aliexpress.com/item/1005005445851704.html)

---

## 🛠️ Setup Instructions

### 1. Install the APK

#### A. Enable Installation from Unknown Sources

On your Android phone:

1. Go to `Settings > Security` (or `Apps > Special app access > Install unknown apps`)
2. Choose your browser or file manager.
3. Enable **“Allow from this source”**

#### B. Download and Install the APK

1. Download the APK from the [GitHub release page](https://github.com/michaloblastni/local-neural-monitoring-android/releases).
2. Open it using your file manager or browser.
3. Tap **Install**
4. If warned that the app is unsigned, confirm by tapping **Install anyway**

### 2. Connect the EEG-SMT to Your Phone

- Plug the EEG-SMT into the OTG adapter/hub.
- Plug the OTG adapter into your smartphone.
- If prompted on your phone, **grant USB permission** to access the device.

---

## 📉 What the App Does

- Opens the USB serial connection to EEG-SMT  
- Displays **2 EEG channels** in real time  
- Includes a **Start/Stop Recording** button (functionality is not yet working. It will be fixed in future releases)

---

## 🐞 Troubleshooting

- If the app doesn’t detect the device:
  - Reconnect the EEG-SMT and restart the app.
  - Make sure you granted USB access when prompted.
- If no graph appears:
  - Check that the EEG-SMT is outputting valid data (electrodes need to be connected).

---

## ✅ Tested With

- Olimex EEG-SMT device  
- Android 15
- USB-C OTG adapter

---

## ⚠️ Disclaimer

This application is **experimental** and not approved for medical diagnostics or treatment. Use it at your own risk.

---

## 📄 License

This project is released under the [MIT License](LICENSE).

