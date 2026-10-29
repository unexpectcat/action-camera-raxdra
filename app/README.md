# Action Cam app

Android app (Kotlin + Compose Multiplatform, same toolchain as Unplanner) for the camera.
A Linux desktop build of the same code is used for screenshots and quick tests.

- **Splash**: logo placeholder (`ui/common/Components.kt` → `Logo`, launcher icon in
  `androidMain/res/drawable/ic_launcher_foreground.xml`). Swap both when the real logo exists.
- **Cameras**: found by UDP broadcast + a sweep of the local /24. Pull down to rescan, **+** adds one by IP.
  Tap = camera screen, hold = its videos.
- **Camera screen**: last photo, **Send photo** asks the camera for a new one, **REC** top right
  starts/stops recording. Videos are named with the phone's local time (`2026-10-29_12-30-05.mp4`).
- **Videos**: listed from the camera, downloaded only when tapped (tap again cancels, tap when saved plays).
  Saved to `Movies/ActionCam` on the phone.

Talks to `server/camd.py` on the camera (HTTP on 8080, discovery on UDP 41234).

## Build

Gradle needs JDK 17–21: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`

| What | Command |
|---|---|
| Install on the phone over USB | `./gradlew :composeApp:installDebug` |
| APK | `./gradlew :composeApp:assembleDebug` → `composeApp/build/outputs/apk/debug/` |
| Desktop window | `./gradlew :composeApp:run` |
| Screenshots of every screen | `./gradlew :composeApp:screenshots` → `composeApp/build/screenshots/` |
| Tests | `./gradlew :composeApp:desktopTest` |
| Tests against a running camd | `CAMD_HOST=127.0.0.1 ./gradlew :composeApp:desktopTest` |

## Test camera on the laptop

No board needed: `CAMD_NAME="Laptop test cam" CAMD_MEDIA_DIR=/tmp/camtest python3 ../server/camd.py`
serves a generated test picture and test videos. A phone on the same Wi-Fi finds it.
