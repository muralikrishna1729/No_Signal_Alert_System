📡 No Signal Alert System – Android App
Real-time network signal monitoring, GPS-based weak-signal logging, heatmap visualization & alerts
📝 Project Overview

No Signal Alert System is an Android application designed to continuously monitor mobile network signal strength and GPS location in real time.
When the signal becomes critically weak (default threshold: –115 dBm), the app:

✔ Shows an alert notification
✔ Saves the weak-signal location to a Room database
✔ Displays the logs in a dedicated screen
✔ Allows exporting all logs to CSV
✔ Plots a Heatmap on Google Maps to visualize poor network zones

This project is built for academic evaluation and real-world usage in rural/low-coverage locations.

🚀 Features
📶 Real-Time Signal Monitoring

Reads device cellular signal strength continuously

Works on API level 26+ (Android 8)

Backward-compatible using reflection (no deprecated crashes)

📍 GPS Tracking

High-accuracy location updates every few seconds

Auto-updates UI with the latest latitude/longitude

⚠ Weak Signal Detection

Detects weak signals below –115 dBm

Plays alert notification (sound/vibration enabled)

Only alerts once per minute to prevent spam

🗂 Weak Signal Logging

Saves timestamp, dBm, latitude, longitude

Stored in Room database (signal_db)

Automatic cooldown prevents overlogging

📄 Logs Screen

View all logs in RecyclerView

Delete single log / delete all logs

Export logs to CSV (easy for analysis in Excel)

🗺 Heatmap Visualization

Displays all weak-signal points on Google Maps

Uses Google Maps Utils HeatmapProvider

Helps identify signal blackspots

🔀 Bottom Navigation
Home	Logs	Heatmap
Signal + GPS UI	Full log history	Google Maps visualization
🛠 Tech Stack
Languages

Kotlin

Frameworks & APIs

Android Jetpack

Room Database

Google Play Services (Location, Maps)

Google Maps Utils (Heatmap)

Coroutines + LifecycleScope

Architecture
Fragments (UI Layer)
│
├── HomeFragment     → Signal & GPS monitoring
├── LogsFragment     → RecyclerView + DB + CSV
└── MapFragment      → Heatmap visualization
│
Data Layer
│
├── Room Database
│   ├── WeakSignalEntity
│   ├── WeakSignalDao
│   └── AppDatabase
│
Services
│
└── SignalForegroundService → Runs monitoring in background

📸 Screenshots (Add After Running App)

Create a screenshots/ folder in your repo and add images:

📁 screenshots/
   ├── home.png
   ├── logs.png
   ├── csv_export.png
   ├── heatmap.png
   └── notification.png


Then link them here:

📌 Home Screen

Real-time signal strength + location


📌 Logs Screen

List of all weak-signal events


📌 CSV Export

CSV file created in /storage/emulated/0/Android/data/.../files/


📌 Heatmap Visualization

Weak-signal clusters shown on Google Maps


📌 Notification

Weak-signal alert in status bar


📦 Installation & Setup
1. Clone the Repo
git clone https://github.com/YOUR_USERNAME/NoSignalAlertSystem.git
cd NoSignalAlertSystem

2. Open in Android Studio

Android Studio Ladybug / Jellyfish recommended

File → Open → Select project folder

3. Add Google Maps API Key

Open:

app/src/main/AndroidManifest.xml

Add inside <application> block:

<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE" />

4. Install Dependencies

Android Studio → Sync Project with Gradle Files

5. Run App on Real Device

Signal APIs do not work on emulators.

Use a real Android phone:

Enable Developer Mode

USB Debugging

Connect via USB / WiFi

📤 Exporting CSV

Exports file to:

Android/data/com.example.nosignalalertsystem/files/weak_signal_logs.csv


Open via:

Files app → Android/data → select your app → files

Or connect to PC

🧪 Testing Checklist for Review

Before submitting for academic review, test the following:

Home Screen

 Signal updates in real time

 Location updates

 Start/Stop Monitoring works

 Notification appears for weak signal

Logs Screen

 Logs are saved in DB

 Logs display correctly

 Single log delete

 Delete all logs

 Export CSV works

Heatmap

 Loads Google Map

 Heatmap tiles appear

 Shows weak-signal clusters

Service

 Runs in background

 Foreground notification visible

📘 Academic Explanation (Short Version)

This app continuously monitors cellular signal strength using Android’s PhoneStateListener.
When the signal drops below a critical threshold, it logs the GPS coordinates using Google Play Services FusedLocationProvider.

Each weak signal event is stored in a Room Database.
Users can view logs, export them to CSV, and visualize frequency clusters using Google Maps Heatmap API.

📚 Future Enhancements

User-customizable signal threshold

Upload logs to cloud (Firebase)

Offline map caching

Battery usage optimization

Carrier-wise analysis (e.g., Airtel/Jio/VI)

© License

MIT License (recommended for academic/public projects)
