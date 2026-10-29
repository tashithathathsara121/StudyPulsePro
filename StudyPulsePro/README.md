# StudyPulse Pro

An Android study-management app that follows a daily timetable, locks you into a focus screen during study sessions,
silences distracting notifications (calls still ring), and sounds an alarm when each break ends.

![Platform](https://img.shields.io/badge/platform-Android%208%2B-black) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-FFD600) ![License](https://img.shields.io/badge/license-MIT-lightgrey)

<!-- Add screenshots to /docs and link them here -->
<!-- ![Portrait](docs/portrait.png) ![Landscape](docs/landscape.png) -->

## Features
- Timetable from 8:30 AM to 12:00 AM, editable in-app (Room database)
- Large animated countdown ring; separate portrait and landscape layouts
- **Standard Focus Mode**: immersive full screen, Back disabled, optional screen pinning
- **Managed Kiosk Mode**: Lock Task Mode via Device Owner (Home/Recents locked, released on breaks)
- Calls-only Do Not Disturb; previous settings saved and restored
- Exact alarms (`setAlarmClock`) plus a foreground service, so timing survives screen-off and reboot
- Built-in study browser with an approved-sites list during focus; PDFs via the Google viewer
- Daily session progress saved automatically
- Black / yellow "Liquid Glass" UI in Jetpack Compose

## Architecture
| Layer | Files | Role |
|---|---|---|
| UI | `ui/` | Compose screens: session view, schedule, settings, setup, browser |
| ViewModel | `MainVM.kt` | Exposes timetable, clock-driven snapshot, settings, connectivity |
| Core | `core/Engine.kt` | Pure function: (timetable, time) → current/next slot. No stored timer, so rotation or restart cannot desync it |
| Core | `core/Controller.kt` | Idempotent sync: DND, progress, alarm on transition, schedules next alarm |
| Core | `core/Dnd.kt`, `Lock.kt`, `Alarm.kt`, `Notif.kt`, `Services.kt` | Platform integrations |
| Data | `data/` | Room entities/DAO, DataStore preferences, sample timetable |

Stack: Kotlin, Jetpack Compose, Material 3, MVVM, Room, DataStore, AlarmManager, NotificationManager, DevicePolicyManager.

## Build
- Android Studio (JDK 17): open the folder, sync, **Build → Build APK(s)**.
- Or push to GitHub: the **Build APK** workflow produces `StudyPulse-Pro.apk` as an artifact.

Output: `app/build/outputs/apk/release/StudyPulse-Pro.apk`

## Setup on device
Open the in-app **Setup** screen and grant Do Not Disturb access, notifications, display-over-apps, full-screen alerts and unrestricted battery.
On Xiaomi/HyperOS also enable Autostart and keep auto-rotate on.

### Managed Kiosk (optional)
1. Remove all accounts from the phone; enable USB debugging (and "USB debugging (Security settings)" on Xiaomi).
2. `adb shell dpm set-device-owner com.studypulse.pro/.AdminReceiver`
3. Enable **Managed Kiosk Mode** in Settings.

Release: Settings → Release Device Owner, or `adb shell dpm remove-active-admin com.studypulse.pro/.AdminReceiver`.

## Known limitations
- Without Device Owner, Android does not let a normal app block Home/Recents; Standard mode deters but cannot fully prevent leaving.
- Android only lets apps silence other apps' notifications, not remove them.
- Not yet covered by automated tests; behaviour verified on device only.

## Emergency exit
Press and hold the 🔒 label on the countdown and type `EXIT`. Focus lock releases until the session ends.

## License
MIT
