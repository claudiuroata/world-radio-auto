# World Radio Auto 📻🚗

An Android online radio streaming application optimized for **Android Auto** and mobile devices, connecting users to over 45,000 live radio stations worldwide powered by the [Radio Browser API](https://www.radio-browser.info/).

---

## 🌟 Key Features

- **Optimized for Android Auto**: Fully compliant with Google's Automotive Media App standards using modern **AndroidX Media3 (`MediaLibraryService`)**.
- **Car Head-Unit Navigation**: The top-level root hierarchy exposes 5 dedicated categories:
  1. 🕒 **Recent** *(First Tab)*: Quickly resume recently played radio stations.
  2. ⭐ **Favorites** *(Second Tab)*: Instant access to starred stations with one tap from the dashboard.
  3. 📍 **Local**: Live broadcast stations detected for your locale/country.
  4. 🌍 **World / Filter by Country**: Browse global stations organized by country and region.
  5. 🔥 **Popular**: Highest voted and top-streamed stations across the globe.
- **In-Car Controls**:
  - Head-unit custom action button (`ACTION_TOGGLE_FAVORITE`) to star/unstar stations while driving.
  - Ignition / Bluetooth reconnect playback resumption (`onPlaybackResumption`).
  - Audio focus & noise handling (smooth pause when disconnected from car Bluetooth or on GPS/call interrupts).
- **Mobile Experience**:
  - Built with Jetpack Compose & Material 3.
  - Floating MiniPlayer with live buffer indicator, waveform animation, and quick controls.
  - Search any radio station, genre, or tag.
  - Offline cache and favorites persistence via Room Database.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+ (Coroutines & StateFlow)
- **Audio & Automotive Engine**: AndroidX Media3 1.5.1
  - `MediaLibraryService` & `MediaLibrarySession`
  - `ExoPlayer` with HLS, AAC, MP3, Icecast & Shoutcast live streams
  - `OkHttpDataSource`
- **UI Framework**: Jetpack Compose with Material 3 (dark automotive theme)
- **Local Persistence**: AndroidX Room (`StationDao`, `RadioDatabase`)
- **Networking**: Retrofit 2 + Moshi + OkHttp
- **Image Loading**: Coil 2 for remote station favicons and logos

---

## 📁 Project Structure

```
world-radio-auto/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Declares MediaLibraryService & Auto metadata
│   │   ├── res/
│   │   │   └── xml/automotive_app_desc.xml # Declares media template capability for Android Auto
│   │   └── java/com/worldradio/auto/
│   │       ├── RadioApp.kt              # App singleton & dependency setup
│   │       ├── data/
│   │       │   ├── model/               # Station & Country models
│   │       │   ├── remote/              # Radio Browser API Retrofit client
│   │       │   ├── local/               # Room Entity, DAO, and Database
│   │       │   └── repository/          # Unified RadioRepository
│   │       ├── media/
│   │       │   ├── MediaItemConverter.kt # Bridges domain models with Media3 MediaItems
│   │       │   └── RadioMediaService.kt  # MediaLibraryService handling Android Auto IPC
│   │       └── ui/
│   │           ├── MainActivity.kt      # Mobile edge-to-edge Compose activity
│   │           ├── theme/               # Dark automotive theme & palette
│   │           ├── components/          # MiniPlayer, StationListItem, animated waveform
│   │           ├── screens/             # Recent, Favorites, Local, World, Popular, HomeScreen
│   │           └── viewmodel/           # RadioViewModel managing playback & media browsing
└── gradle/
    └── libs.versions.toml               # Modern Gradle Version Catalog
```

---

## 🚘 How Android Auto Integrates with This App

In Android Auto, media apps do not draw custom arbitrary UI on the car screen; instead, they integrate with the car's native automotive media template via **AndroidX Media3**:

1. **Root Tabs**:
   In `RadioMediaService.kt`, `onGetChildren(parentId = "[ROOT]")` returns 5 browsable `MediaItem`s. Android Auto renders these 5 items directly as the navigation tabs across the car screen:
   - `[TAB_RECENT]`
   - `[TAB_FAVORITES]`
   - `[TAB_LOCAL]`
   - `[TAB_WORLD]`
   - `[TAB_POPULAR]`
2. **Deep Browsing**:
   Selecting **World** returns country folders. Tapping a country returns playable station streams for that country.
3. **In-Car Actions**:
   `COMMAND_TOGGLE_FAVORITE` is registered as a custom action in `MediaLibrarySession`, displaying a Star icon on the car display to toggle favorites without leaving the media screen.

---

## 🧪 Testing with Android Auto Desktop Head Unit (DHU)

To test the Android Auto interface on your development machine:

1. Enable Developer Options in the **Android Auto app** on your test phone:
   - Open Android Auto Settings -> Tap "Version" 10 times -> Enable "Start head unit server".
2. Forward the port using ADB:
   ```bash
   adb forward tcp:5277 tcp:5277
   ```
3. Launch the Android Auto Desktop Head Unit (DHU) from Android SDK:
   ```bash
   %LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe
   ```
4. **World Radio Auto** will appear in the media drawer on the DHU display!

---

## 📄 License

Copyright (C) 2026 Claudiu Roata

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** — see the [LICENSE](LICENSE) file for full details.

