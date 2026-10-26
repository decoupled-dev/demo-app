# Android 17 AAOS — Customer Demo Showcase Guide

Guidance for presenting this project as an **Android 17 AAOS** customer demo: what to show today, which platform capabilities to claim, and what to add for maximum impact.

This app is a strong **Android 17 AAOS design + Scalable UI** demo — not a full vehicle stack. Pitch that clearly, then layer high-impact additions.

---

## What to showcase today (customer flow ~12–15 min)

### 1. Android 17 Advanced Windowing / Scalable UI

Your strongest Android 17 story.

- Map under apps, floating rail, media overlay, parking assistant, split `app_panel`
- Talk track: *“Android 17 Scalable UI — multi-panel cockpit, map stays live, OEM-customizable SystemBars via RRO.”*
- Use `:scalable-ui-rro` + Adaptive Space widget; install RROs if hardware allows

### 2. Material 3 Expressive for cars

Use the [`motion.md`](../motion.md) presentation script.

- Dashboard container transforms → Climate / Media / Nav / Vehicle
- Shape morph, shared elements, layout morph (Sport mode)
- **Parked → Driving** clamps motion to Standard (safety / distraction)

### 3. OEM brandability

- Stock M3 vs **Horizon** OEM theme — same screens, different brand DNA
- Message: *“Compose design system + tokens; OEMs own look without forking UX.”*

### 4. IVI feature surfaces (simulated is fine if labeled)

- Climate, Media, Nav (OpenStreetMap via MapLibre + demo route), Vehicle energy modes, Glanceables

### 5. AAOS app model

- Distraction-optimized activities, `APP_MAPS` / `geo:` / navigate intents, automotive hardware feature

**Lead with Scalable UI + Expressive + OEM theme.** Treat climate/media/vehicle as UX demos unless you wire real Car APIs.

---

## Android 17 / AAOS 26Q2 angles you can claim

| Platform capability | Your demo today | Customer takeaway |
|---|---|---|
| Scalable UI / Advanced Windowing | Strong (RRO + in-app) | Dynamic multi-panel IVI |
| Scalable System UI / modular CarSystemUI | Partial (RRO docs) | Lower OEM SystemUI cost |
| MUMD (multi-user, multi-display) | Missing | Passenger / rear-seat story |
| HUNs for Scalable UI | Missing | Branded notifications / HVAC HUN |
| Display Safety / HAR / DriverUI (cluster) | Missing | Unified cockpit + safety layers |
| SDV beyond IVI | Narrative only | Android powering more domains |
| Vehicle property availability | Missing (mocks) | Real VHAL / property status |
| Audio focus / gain callback | Missing | Safe media / volume policy |
| AppFunction APIs | Missing | Agent / voice → app actions |

---

## What to add for “wow” (priority order)

### Tier 1 — biggest demo lift, fits this repo

1. **Live Scalable UI choreography** — one-tap: map → media overlay → apps split → parking assistant → collapse (scripted “cockpit ballet”)
2. **Passenger / dual-zone mock** — driver-restricted UI vs full passenger panel (MUMD story without full platform)
3. **Cluster companion strip** — speed / gear / telltales + media/nav glance (Display Safety / DriverUI narrative)
4. **Real CarPropertyManager path** (or clean simulator toggle) — gear, speed, HVAC, battery with “Simulated / Live” badge
5. **Heads-up notification panel** — branded HUN for nav, call, HVAC (Scalable UI HUN story)

### Tier 2 — makes it feel product-ready

6. **MediaSession** — real audio focus + now-playing that survives overlays
7. **Voice / AppFunction demo** — “Set temp to 22” / “Play jazz” driving UI
8. **Camera2 parking / surround** — replace static parking overlay with live preview
9. **Extract `:component` library** — OEM kit they can take home
10. **Theme studio live switcher** — accent, type, shape radius, motion scheme in one panel

### Tier 3 — executive theater

11. **Night / day / charging ambient themes** that recolor the whole cockpit
12. **OTA / SDV storyboard screen** — “same software → cluster + body + IVI”
13. **Proactive AI card** — traffic + charge stop + calendar (keep glanceable, not chatty)

---

## Recommended customer narrative

> “This is an Android 17 AAOS reference cockpit: Scalable UI windowing, Material 3 Expressive motion tuned for driving safety, and an OEM brand layer. The same platform story extends to multi-display, modular SystemUI, and software-defined vehicle domains.”

**Do not oversell** real VHAL, cluster HAR, or SDV unless those are on the target board — call out simulation vs platform.

---

## Minimal “wow sprint” (implemented)

| Item | Where to show |
|------|----------------|
| Scripted Adaptive Space scene transitions | Adaptive Space → **Play demo** (map → media → split → parking → collapse) |
| Driver vs passenger dual-zone | Apps dashboard → **Dual Zone** widget |
| Cluster glance + HUN panel | Driving sidebar speed/gear/limit; top-bar **Notifications** → Heads-up panel |

Those three map directly to **Android 17 Scalable UI + MUMD + Display Safety** talking points.

### Polish shipped with the sprint

- **Simulated** badges on Climate / Media / Nav / Vehicle / Adaptive Space / Dual Zone / Settings
- Cluster speed/gear follows Driving UX (Parked / Driving / Restricted)
- Motion Studio opened from Settings
- Floating Settings opens the Settings widget (shared activity `DashboardViewModel`)
- Restricted banner on the Apps hub

---

## Related docs

| Doc | Purpose |
|-----|---------|
| [`../motion.md`](../motion.md) | 5–8 min Material 3 Expressive presentation script |
| [`motion/expressive-features.md`](./motion/expressive-features.md) | Feature-first motion talking points |
| [`ui-stack-comparison.md`](./ui-stack-comparison.md) | Views vs Compose stack decision matrix |
| [`../README.md`](../README.md) | Adaptive Space architecture, map intents, RRO install |
| [`../scalable-ui-rro/README.md`](../scalable-ui-rro/README.md) | Scalable UI RRO install |
