# Vehicle Screen — Motion

Vehicle is the deepest motion surface — layout morphing, dynamic color, systems feedback, and a built-in **Motion Studio**.

[← Back to README](../../README.md)

---

## Battery gauge

**Triggers:** Change battery %, toggle charging, cycle demo values (tap gauge).

| Property | Token |
|----------|-------|
| Arc fill progress | `slowSpatialSpec()` |
| Range ring progress | `defaultSpatialSpec()` |
| Arc color (charge / level) | `defaultEffectsSpec()` |
| Gauge shape (Sport / charging) | `slowSpatialSpec()` |
| Charging pulse (scale) | Infinite `tween` — decorative loop |
| Background orb drift | Infinite `tween` — ambient motion |

**What to say:** *“Progress arcs use spatial springs; color shifts use effects. Charging adds ambient pulse loops on top of the M3 token system.”*

---

## Drive mode selector

**Trigger:** Eco / Comfort / Sport.

**Motion:** Primary pill slides across three segments (same pattern as climate airflow).

**Token:** `slowSpatialSpec()`

---

## Drive-mode layout rearrangement

**Trigger:** Switch drive mode.

**Motion:** The three-column row **reweights** smoothly:

| Mode | Layout behavior |
|------|-----------------|
| Eco | Energy column widens; Motion Studio hidden; selector moves to top |
| Comfort | Balanced weights; Motion Studio visible below stats |
| Sport | Side column widens; Motion Studio dominates |

**Tokens:**
- Column `weight()` values → `defaultSpatialSpec()`
- Drive selector Y position → `slowSpatialSpec()`
- Systems panel top/bottom padding → `slowSpatialSpec()`
- Side column height → `animateContentSize` with `defaultSpatialSpec()`
- Motion Studio show/hide → `expandVertically` / `shrinkVertically` + fade

**What to say:** *“Drive mode doesn’t just recolor — the whole layout morphs in place. Spatial tokens animate weights and offsets; effects tokens handle fade for panels entering or leaving.”*

---

## Dynamic vehicle color scheme

**Trigger:** Drive mode, battery level, charging.

**Motion:** Screen tint cross-fades.

**Token:** `defaultEffectsSpec()`

---

## Vehicle systems panel

**Triggers:** Select a system, change regen, toggle charging.

| Element | Motion | Token |
|---------|--------|-------|
| Health score badge color | Color cross-fade | `defaultEffectsSpec()` |
| Power flow strip strength | Width fraction | `slowSpatialSpec()` |
| Flow strip color | Color cross-fade | `defaultEffectsSpec()` |
| Flow node alpha | Fade | `defaultEffectsSpec()` |
| System row progress bar | Fill fraction | `defaultSpatialSpec()` |
| Row selection background | Color cross-fade | `defaultEffectsSpec()` |

---

## Trip stats card

**Trigger:** Switch to Sport mode.

**Motion:** Stats card corners morph to expanded radii.

**Token:** `defaultSpatialSpec()`

---

## Odometer counter

**Trigger:** (Demo data updates)

**Motion:** Same directional vertical slide as climate temperature.

**Token:** `defaultSpatialSpec()`

---

## Motion Studio (Sport / Comfort)

**Location:** Vehicle detail → right column (prominent in Sport).

**Purpose:** Live comparison of all six motion tokens on one screen.

**How to demo:**
1. Switch to **Sport** or **Comfort** drive mode.
2. Tap **Replay** to run the preview animation.
3. Tap each token cell (Spatial, Fast, Slow, Effects, Snappy, Gentle).
4. Optionally switch scheme chips (Standard / Expressive / Custom) — Vehicle screen uses a **local** `MaterialTheme.motionScheme` override.

| Cell | Animation shown |
|------|-----------------|
| Spatial | Dot slides horizontally |
| Fast | Dot slides with `fastSpatialSpec()` |
| Slow | Dot slides with `slowSpatialSpec()` |
| Effects | Dot scales in place |
| Snappy | Dot scales with `fastEffectsSpec()` |
| Gentle | Dot scales with `slowEffectsSpec()` |

**What to say:** *“Motion Studio is our sandbox — same six tokens Material 3 defines, side by side. Switch schemes to feel Standard vs Expressive vs our OEM Custom tuning.”*

---

## Source files

| Area | Files |
|------|-------|
| Screen | `app/.../vehicle/VehicleScreen.kt`, `VehicleDriveModeLayout.kt` |
| Components | `app/.../vehicle/components/VehicleEnergyCockpit.kt`, `VehicleSystemsPanel.kt`, `VehicleMotionStudio.kt`, `MorphingDriveModeSelector.kt` |
| Shapes | `app/.../theme/MorphingRoundedShape.kt` |
