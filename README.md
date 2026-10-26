# Design — AAOS demo

Android Automotive (AAOS) design system demo built with Kotlin and Jetpack Compose.

## Build

```bash
chmod +x ./gradlew
./gradlew assembleDebug testDebugUnitTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Map

Home, navigation, and Adaptive Space use [MapLibre Native](https://maplibre.org/) with the free [OpenFreeMap](https://openfreemap.org/) dark style (OpenStreetMap data). No API key. The device needs a network connection to load tiles; attribution stays on the map.

## Architecture — Adaptive Space (Scalable UI)

Sealed home layout lives in **`:scalable-ui-rro`** (Android 17 Advanced Windowing):

| Panel | Role |
|-------|------|
| `map_panel` | Full-bleed stock MapsPlaceholderActivity (green) |
| `depth_scrim` | DecorPanel Z-depth between map and overlays |
| `widget_panel` | Floating left rail (`DrivingRailActivity`) |
| `media_overlay` | Slide-in media controller (map stays live) |
| `parking_assistant` | Slide-in parking overlay |
| `app_panel` | Transient / split-screen apps (zero-stutter resize) |
| `status` / `nav` | Floating Scalable UI `<SystemBar>` |

- RRO + install: [`scalable-ui-rro/README.md`](scalable-ui-rro/README.md)
- Prebuilt APKs: [`prebuilt/`](prebuilt/) — sync `./scripts/sync-prebuilts.sh` / `.\scripts\sync-prebuilts.ps1`, install `./scripts/install-prebuilts.sh` / `.\scripts\install-prebuilts.ps1`
- CarSystemUI follow-ups: [`scalable-ui-rro/CARSYSTEMUI.md`](scalable-ui-rro/CARSYSTEMUI.md)
- In-app demo: **Adaptive Space** widget on the Apps dashboard

`MainActivity` remains an in-process Compose demo of the same home.

### Intent filters

`MapActivity` accepts:

| Action | Purpose |
|--------|---------|
| `android.intent.category.APP_MAPS` | AAOS map app registration |
| `com.test.design.action.OPEN_MAP` | Direct launch — opens **Navigation** (same as Search maps) |
| `android.intent.action.VIEW` + `geo:` | Display map at coordinates |
| `androidx.car.app.action.NAVIGATE` + `geo:` | Navigation entry (shows demo route) |

### Launch from code

```kotlin
import com.test.design.presentation.ivi.map.MapIntents

startActivity(MapIntents.openMap(context))
startActivity(MapIntents.openMap(context, showRoute = true))
```

Optional intent extras:

- `com.test.design.extra.SHOW_ROUTE` (boolean) — overlay the demo route
- `com.test.design.extra.ZOOM` (double) — initial zoom level (default `14.5`)

### Launch via adb

```bash
adb shell am start -a com.test.design.action.OPEN_MAP \
  -n com.test.design/.presentation.ivi.map.MapActivity

adb shell am start -a android.intent.action.VIEW \
  -d "geo:37.7749,-122.4194" \
  -n com.test.design/.presentation.ivi.map.MapActivity

adb shell am start -a androidx.car.app.action.NAVIGATE \
  -d "geo:37.7749,-122.4194" \
  -n com.test.design/.presentation.ivi.map.MapActivity
```

### Return to main screen

From `MapActivity`:

- Tap the **Home** icon at the top of the right-side map controls
- Press the system **Back** button when no widget is expanded

Both open `MainActivity` (driving home). The app grid in the sidebar opens the full widget dashboard on `MainActivity`.

```kotlin
startActivity(MapIntents.openMain(context))
startActivity(MapIntents.openMain(context, openDashboard = true))
```

## Assistant

Translucent immersive assistant (fullscreen overlay). Full adb cheat sheet: [`docs/assistant-adb.md`](docs/assistant-adb.md).

```bash
adb shell appops set com.test.design SYSTEM_ALERT_WINDOW allow
adb shell pm grant com.test.design android.permission.RECORD_AUDIO

adb shell am start -a com.test.design.action.OPEN_ASSISTANT \
  -n com.test.design/.presentation.assistant.VirtualAssistantActivity
```

Or: `.cursor/scripts/launch-assistant.sh`

## Floating system bars (adb)

In-app floating top/bottom bars are **hidden by default**. Show them while the demo is running:

```bash
adb shell am broadcast -a com.test.design.action.SHOW_SYSTEM_BARS \
  -n com.test.design/.presentation.ivi.dashboard.FloatingSystemBarsReceiver

# or toggle
adb shell am broadcast -a com.test.design.action.TOGGLE_SYSTEM_BARS \
  -n com.test.design/.presentation.ivi.dashboard.FloatingSystemBarsReceiver
```

Full cheat sheet: [`docs/system-bars-adb.md`](docs/system-bars-adb.md).

### Scalable UI action (system intent)

In `scalable_ui_actions.xml`, dispatch the map activity when a panel event fires:

```xml
<Action intent="intent:#Intent;action=com.test.design.action.OPEN_MAP;component=com.test.design/.presentation.ivi.map.MapActivity;end">
    <Event id="_System_TaskOpenEvent" panelId="@id/map_panel" />
</Action>
```

## Cloud agents

See [AGENTS.md](AGENTS.md) for Cursor Cloud build, emulator, and CI instructions.
