# Voltra Controller

Voltra Controller is a local-first Android app for controlling a VOLTRA over Bluetooth without an account, subscription, or cloud login.

Source repository: [github.com/dylanmaniatakes/Beyond-Power-Voltra-Android](https://github.com/dylanmaniatakes/Beyond-Power-Voltra-Android)

Current app version: **Beta 1.3**

This app is built from BLE captures, public protocol evidence, and community testing. It is beta software for a resistance device, so start light, keep unload easy to reach, and verify the VOLTRA screen when testing new modes.

## What It Does

- Connects directly to one VOLTRA over Bluetooth.
- Controls supported workout modes from Android.
- Mirrors live device state, battery, target load, reps, sets, phase, and decoded mode telemetry.
- Automatically raises the app's weight ceiling to 250 lb only when the VOLTRA reports support for it.
- Exports diagnostics for protocol capture, bug reports, and community support.
- Optionally publishes local MQTT state or exposes a local HTTP gateway for self-hosted integrations.
- Checks GitHub releases from the More screen so sideload users can find newer APKs.

## Supported Modes

### Weight Training

- Target weight
- Load, unload, and Hold to Direct Load
- Chains, inverse chains, eccentric, assist, and resistance experience
- Weight presets

### Resistance Band

- Force target
- Load, unload, and Hold to Direct Load
- Standard / inverse mode
- Power law / logarithm curve
- Band Length or ROM setup

### Damper

- Damper factor selection
- Resistance experience
- Load, unload, and Hold to Direct Load
- Peak force, peak power, and time-to-peak summaries when available

### Isokinetic

- Target speed
- Eccentric settings
- Constant resistance
- Max eccentric load
- Load, unload, and Hold to Direct Load

### Isometric

- Mode entry and ready/load flow
- Live force graph
- Peak force, time, time-to-peak, impulse, and RFD windows
- RFD 0-50ms, 0-100ms, 0-150ms, 0-200ms, and peak RFD 100ms

### Custom Curve

- Four-point curve editor
- Resistance range
- Range of motion
- Local curve presets
- Apply, save, edit, and delete flows

### Cardio

- Row mode with Just Row and distance presets
- Ski mode uses the stock Cardio activity selector before entry/start
- Live distance, time, pace, average pace, strokes, SPM, and force graphing

Ski and Row share the VOLTRA Cardio workout state and telemetry stream. Android writes the recovered stock selector (`0x54F5`: Rowing `0`, Ski `1`) before entering or starting Cardio so the slider and telemetry stay on the selected activity.

### Sled Pull

Sled Pull has a Home-screen placeholder in Beta 1.3. It is intentionally disabled until beta firmware and stock-app traffic confirm selector, load, and telemetry behavior.

## More Screen

The More screen contains:

- Theme and control defaults
- Device name and startup image tools
- Weight preset library
- Workout history CSV export
- Diagnostics copy/share
- GitHub release update check
- MQTT Sensor settings
- HTTP Gateway settings

## Optional Local Integrations

MQTT Sensor and HTTP Gateway are disabled by default. They are intended for local networks, dashboards, Node-RED, Home Assistant-style tooling, and self-hosted experiments.

The app does not require these integrations for normal Bluetooth control.

## Privacy

Voltra Controller does not require:

- an account
- a subscription
- mandatory cloud sync
- a backend service

Local preferences, saved presets, workout history, and diagnostics remain on the Android device unless you explicitly export/share them or enable MQTT/HTTP.

## Safety

- Start new captures and new firmware tests at low weight.
- Confirm mode and load state on the VOLTRA screen.
- Keep unload easy to reach.
- Do not test unknown beta modes under tension.
- Export diagnostics immediately after a useful success or failure.

## Capture-Only Work

These areas are visible or partially scaffolded but should not be treated as fully supported yet:

- Sled Pull
- profile/user switching
- any unverified beta-only selector or load path

See [Capture Checklist](./CAPTURE_CHECKLIST.md) and [Protocol Notes](./PROTOCOL_NOTES.md) for the current evidence trail.

## Project Docs

- [Release Notes](./RELEASE_NOTES.md)
- [Privacy Policy](./PRIVACY_POLICY.md)
- [HTTP Gateway](./HTTP_GATEWAY.md)
- [Play Store Beta Flow](./PLAYSTORE_BETA.md)
- [Wishlist Status](./WISHLIST_STATUS.md)
- [Capture Checklist](./CAPTURE_CHECKLIST.md)
- [Protocol Notes](./PROTOCOL_NOTES.md)
- [SDK Research Notes](./SDK_RESEARCH_NOTES.md)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/W7W41QWZNC)
