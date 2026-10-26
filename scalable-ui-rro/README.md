# Design Adaptive Space — Scalable UI RRO

Production-level **Android 17 Scalable UI / Advanced Windowing** overlay for the
Design Adaptive Space home: Map-Under-Apps, dynamic secondary overlays, and
zero-stutter split resize.

```
┌──────────────────────────────────────────────────────────┐
│  status  (floating SystemBar, Z 200)                     │
├──────┬───────────────────────────────────────────┬───────┤
│      │                                           │ media │
│ rail │     map_panel full-bleed (green)          │ /park │
│ L20  │     MapsPlaceholderActivity under rail    │ L40–50│
│      │                                           │       │
├──────┴───────────────────────────────────────────┴───────┤
│  nav  (floating SystemBar, Z 210)                        │
└──────────────────────────────────────────────────────────┘
```

| Panel | Type | Layer | Content |
|-------|------|-------|---------|
| `map_panel` | TaskPanel | 2 | Full-bleed stock MapsPlaceholderActivity (green) |
| `depth_scrim` | DecorPanel | 10 | Z-depth dim between map and overlays |
| `widget_panel` | TaskPanel | 20 | Floating left rail (`DrivingRailActivity`) |
| `media_overlay` | TaskPanel | 40 | Slide-in media controller |
| `parking_assistant` | TaskPanel | 50 | Slide-in parking overlay |
| `app_panel` | TaskPanel | 100 | Transient / split-screen apps |
| `status` | SystemBar | Z 200 | Floating top bar |
| `nav` | SystemBar | Z 210 | Floating bottom bar |

## Adaptive Space behaviors

### Map-Under-Apps (green placeholder)
`map_panel` is full-bleed stock `MapsPlaceholderActivity` (green). Floating
`widget_panel` (DrivingRail, 420dp) sits above it — same pattern as in-app
home. Design `MapActivity` is not used as the map host.

### Dynamic panel transitions
OEM events (wire to `CarSystemBarButton` `selectedEvent` / `unselectedEvent`):

| Event | Effect |
|-------|--------|
| `_Design_OpenMediaOverlay` / `_Design_CloseMediaOverlay` | Slide media overlay |
| `_Design_ToggleMediaOverlay` | Toggle media |
| `_Design_OpenParking` / `_Design_CloseParking` | Slide parking assistant |
| `_Design_ToggleParking` | Toggle parking |

### Zero-stutter split resize
`app_panel` variants `split_narrow` (40%) → `split_mid` (55%) → `split_wide` (70%)
plus `DynamicVariant split_resizing` for continuous drag. Hosted activities declare
`resizeableActivity=true` and broad `configChanges` so Android 17 delivers
`onConfigurationChanged` without restarting the activity.

| Event | Variant |
|-------|---------|
| `_Design_SplitNarrow` | `split_narrow` |
| `_Design_SplitMid` | `split_mid` |
| `_Design_SplitWide` | `split_wide` |
| `_Design_SplitFull` | `opened` |
| `_Design_CloseSplit` | `closed` |

Legacy `CarSystemBarPanel` paths are **off**:
`config_enableTop/Bottom/Left/RightSystemBar=false`. Status/nav are Scalable UI
`<SystemBar>` panels with **12dp side insets + 28dp corners** (floating glass),
not full-bleed legacy strips.

## Build

```bash
./gradlew :scalable-ui-rro:assembleDebug :framework-scalable-rro:assembleDebug :app:assembleDebug
# or sync all APKs into ./prebuilt:
./scripts/sync-prebuilts.sh
```

Windows: `.\scripts\sync-prebuilts.ps1`

## Install (products that already declare these resource names)

Full stack (app + RROs) from `./prebuilt`:

```bash
./scripts/install-prebuilts.sh
```

Windows: `.\scripts\install-prebuilts.ps1`

RRO-only:

```bash
./scalable-ui-rro/install-rro.sh
```

Manual:

```bash
adb root && adb remount
adb push prebuilt/DesignScalableUiRRO.apk /system_ext/overlay/DesignScalableUiRRO.apk
adb push prebuilt/DesignFrameworkScalableUiRRO.apk /system_ext/overlay/DesignFrameworkScalableUiRRO.apk
adb shell chmod 644 /system_ext/overlay/Design*.apk
adb reboot
# after boot:
adb shell cmd overlay enable --user 10 com.test.design.systemui.scalableui
adb shell cmd overlay set-priority --user 10 com.test.design.systemui.scalableui highest
adb shell cmd statusbar carsysui-dispatch-event _System_OnHomeEvent
adb shell cmd statusbar carsysui-dispatch-event _Design_OpenMediaOverlay
```

## Dewd `aosp_tangorpro_car` (interim)

Do **not** enable `com.test.design.systemui.scalableui` on stock Dewd — idmap
cannot add Dewd-only XML names, so `map_panel` stays empty (black) while the
left `widget_panel` rail still launches. Use the in-place patch bridge instead
(`sync-prebuilts` patches **and platform-signs** the APK; an unsigned rebuild is
rejected at boot and Dewd Dynamic disappears from `cmd overlay list`):

```bash
./scripts/sync-prebuilts.sh
./scripts/install-prebuilts.sh --dewd
```

Windows: `.\scripts\sync-prebuilts.ps1` then `.\scripts\install-prebuilts.ps1 -Dewd`

Manual:

```bash
python scalable-ui-rro/scripts/patch_dewd_fullpower.py \
  --input scalable-ui-rro/prebuilt/DewdDynamicAospRRO.orig.apk \
  --output scalable-ui-rro/prebuilt/DewdDynamicAospRRO-design.apk
adb push scalable-ui-rro/prebuilt/DewdDynamicAospRRO-design.apk \
  /system_ext/overlay/DewdDynamicAospRRO.apk
adb install -r prebuilt/app-debug.apk
adb reboot
```

## In-app demo

Open **Adaptive Space** from the Apps dashboard — Compose showcase of
Map-Under-Apps, media/parking overlays, and fluid split resize without SystemUI.

## CarSystemUI work (not RRO)

See **[CARSYSTEMUI.md](CARSYSTEMUI.md)** — glass bar layouts, Dagger suppliers,
clipping, product `window_states` ownership.
