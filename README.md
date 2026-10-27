# Design — AAOS climate & vehicle demo

Android Automotive (AAOS) design system demo built with Kotlin and Jetpack Compose,
stripped down to two screens:

- **Climate** — HVAC temperature, fan, airflow, seat/steering heat (`presentation/ivi/climate`)
- **Vehicle info** — battery/range, tires, trip, drive modes (`presentation/ivi/vehicle`)

Both read live values through `CarPropertyManager` (`data/car`).

## Build

```bash
chmod +x ./gradlew
./gradlew assembleDebug testDebugUnitTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Launch

`MainActivity` hosts both screens behind a navigation rail. Open a specific screen via adb:

```bash
adb shell am start -a com.test.design.action.OPEN_CLIMATE -n com.test.design/.MainActivity
adb shell am start -a com.test.design.action.OPEN_VEHICLE -n com.test.design/.MainActivity
```

Live HVAC/vehicle properties need the privileged car permissions — see
`scripts/install-climate-privapp.ps1` and `scripts/privapp-permissions-com.test.design.xml`.

## Motion notes

- [Climate](docs/motion/climate.md)
- [Vehicle](docs/motion/vehicle.md)

## Cloud agents

See [AGENTS.md](AGENTS.md) for Cursor Cloud build, emulator, and CI instructions.
