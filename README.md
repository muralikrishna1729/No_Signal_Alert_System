# 🛰️ No Signal Alert System – Android App  
A smart Android application that **monitors mobile signal strength**, **tracks GPS location**, and **logs weak signal spots** into a local database.  
It also provides **foreground monitoring**, **real-time alerts**, **logs view**, **CSV export**, and **map heatmap visualization** using Google Maps.

---

## 📌 Features

### 📶 Real-time Signal Monitoring
- Monitors cellular signal strength (dBm value)
- Supports Android **API 26–35**
- Uses:
  - **TelephonyCallback** (Android 12+)
  - **PhoneStateListener** (Android 8–11)

### 📍 GPS Tracking
- Live GPS updates via **Fused Location Provider**
- High accuracy mode

### ⚠️ Weak Signal Alerts
- Notification when the signal drops below **-115 dBm**
- Cooldown system to prevent spam alerts

### 🧭 Foreground Monitoring Service
- Runs monitoring safely in background
- Modern and battery-optimized
- Notification channel supported

### 🗺️ Heatmap Visualization
- Google Maps integration
- Heatmap of weak signal locations
- Color-coded visualization (green → yellow → red)

### 📊 Logs System
- Stores weak signal data using **Room DB**
- View logs in a RecyclerView
- Delete individual logs / delete all
- Export logs as **CSV**

---

## 🏗️ Project Structure
```sh
app/
├── data/
│ ├── WeakSignalEntity.kt
│ ├── WeakSignalDao.kt
│ ├── AppDatabase.kt
│
├── service/
│ └── SignalForegroundService.kt
│
├── ui/
│ ├── home/
│ │ └── HomeFragment.kt
│ ├── logs/
│ │ ├── LogsFragment.kt
│ │ ├── LogsAdapter.kt
│ ├── map/
│ └── MapFragment.kt
│
├── utils/
│ └── SignalStrengthCallback.kt
│
└── MainActivity.kt

```

---

## 🛠️ Tech Stack

| Layer | Technology |
|------|------------|
| UI | XML + Fragments + RecyclerView |
| Navigation | Bottom Navigation + NavHostFragment |
| Background | Foreground Service |
| Database | Room Persistence Library |
| GPS | FusedLocationProviderClient |
| Telephony | SignalStrength APIs |
| Maps | Google Maps + Heatmap Overlay |
| Export | CSV through FileOutputStream |

---

## 🚀 How to Run

### 1️⃣ Clone this repository
```sh
git clone https://github.com/your-username/NoSignalAlertSystem.git
cd NoSignalAlertSystem

```

2️⃣ Open in Android Studio

Android Studio Hedgehog / Jellyfish recommended

Let Gradle sync

3️⃣ Add Google Maps API Key

Create file: 
```sh
app/src/main/res/values/google_maps_api.xml
```
Paste : 
```sh
<string name="google_maps_key">YOUR_API_KEY_HERE</string>
```

4️⃣ Give Permissions on Device

The app will automatically request:
```sh
READ_PHONE_STATE

ACCESS_FINE_LOCATION

ACCESS_COARSE_LOCATION

POST_NOTIFICATIONS (Android 13+)
```
5️⃣ Run on Physical Device

⚠️ Telephony APIs do not work on emulator.

---

📤 CSV Export Format
```sh
timestamp,dbm,latitude,longitude
1735739302323,-118,17.3850,78.4867
1735739319231,-120,17.3844,78.4859
...
```

### 📶  Heatmap Implementation
- Uses Google Maps Utility Library
- Converts DB logs into LatLng list
- Generates heat intensity overlay

### 🗑️ Delete Logs
Features:
   - Swipe-to-delete (RecyclerView)
   - Delete All button
   - Live UI updates with Flow / LiveData
     
### 📚 Future Enhancements
   - Cloud sync (Firebase)
   - Offline map caching
   - Signal graph view
   - Battery saver algorithm
   - Auto SMS alerts when no signal

---

### 📄 License

MIT License – free to use & modify.
```sh
If you want, I can also generate:

✅ `LICENSE`  
✅ `.gitignore`  
✅ Shields.io badges section  
✅ App banner image (ASCII or PNG)  
Just tell me!

```








