# Voltra Controller

Voltra Controller is a local-first Android app for controlling a Voltra over Bluetooth without requiring an account, subscription, or cloud login.

It was made by using BLE capture data and a measure of vibecoding. I have a measure of knowledge in programming and RF technologies but android development is still new to me. 

Source repository: [github.com/dylanmaniatakes/Beyond-Power-Voltra-Android](https://github.com/dylanmaniatakes/Beyond-Power-Voltra-Android)

This project is currently in beta. The core control path is live and usable, while some advanced features are still being refined.

## What Voltra Controller Is

Voltra Controller is designed to feel like a practical daily-use companion for the Voltra:

- connect directly over Bluetooth
- switch into supported workout modes from the app
- change settings from the phone or tablet
- load and unload resistance from the app
- see live device state, battery, and workout counters
- export logs when something needs debugging
- optionally publish live state to MQTT for local automation setups
- optionally expose local HTTP control and state endpoints for relay use on your own network

The app is intentionally local-first. It focuses on direct device control, local preferences, and user-visible diagnostics instead of account flows or cloud features.

## Current Beta Features

### Core Device Control

- Bluetooth scan, connect, reconnect, and disconnect
- local device-first workflow with no mandatory login
- single-device control flow tuned for one Voltra at a time
- battery status and live connection state
- local preferences for units, theme, and control behavior

### Supported Workout Modes

- **Weight Training**
  - target weight control
  - load / unload
  - chains
  - inverse chains
  - eccentric
  - assist
  - resistance experience

- **Resistance Band**
  - resistance control
  - load / unload
  - standard / inverse mode
  - power law / logarithm curve
  - progressive length: Band Length or ROM
  - adjustable band length when Band Length mode is active

- **Damper**
  - damper factor selection
  - resistance experience
  - load / unload

- **Isokinetic**
  - target speed control
  - eccentric settings
  - constant resistance
  - max eccentric load
  - load / unload

- **Isometric Test**
  - mode entry
  - arm / load and unload
  - live test telemetry
  - on-device test values mirrored into the app

- **Custom Curve**
  - four-point curve builder
  - editable graph and sliders
  - resistance limit
  - range of motion
  - local curve presets
  - load / unload

- **Rowing**
  - Just Row
  - preset distances
  - resistance and simulated wear settings
  - live distance, pace, strokes, SPM, and drive-force graphing

### Diagnostics And Export

- live Bluetooth session diagnostics
- raw frame logging
- parsed state logging
- local log export and sharing
- optional MQTT Sensor support from the More screen
- optional HTTP Gateway support from the More screen

### UI And Device Support

- phone layout tuned for modern Android devices
- tablet-aware layout for larger screens
- theme selection with multiple accent choices
- mode-specific control accents for a cleaner control experience

## What Is Still Under Development

Voltra Controller is already usable, but a few areas are still actively being refined:

- kg parity across every editor is still being checked
- some advanced settings are still being promoted from working behavior to polished release behavior
- broader workout history and deeper automation features are planned, but not the main focus of the current beta

## Quick Start

1. Install Voltra Controller on an Android phone or tablet.
2. Power on the Voltra and make sure Bluetooth is enabled on the Android device.
3. Open the app and grant the requested Bluetooth permissions.
4. Scan for the Voltra and connect.
5. Choose a mode from the Home screen.
6. Use the control screen for that mode to adjust settings, then load or unload from the app.
7. Use the More menu for theme settings, MQTT Sensor, HTTP Gateway, and log sharing.

## Using The App

### Home

The Home screen is the launch point for the entire app:

- see the connected Voltra name and battery level
- jump into a supported workout mode
- return to a clean control-first workflow without a crowded navigation experience

### Control Screens

Each control page is designed around the active workout mode rather than around a generic BLE dashboard.

Common behaviors across control pages:

- the top-right control toggles load state for the active mode
- the main control area shows the mode’s primary setting
- secondary settings live in compact Material-style sheets
- rep, set, and phase information are kept close to the main control area when available
- exit returns you cleanly out of the current workout flow

### More Menu

The More menu is where the app keeps the less frequently used tools:

- theme selection
- MQTT Sensor settings
- HTTP Gateway settings
- diagnostics access
- log sharing

## MQTT Sensor

Voltra Controller includes optional MQTT publishing for people building local dashboards or home automation integrations.

MQTT is:

- optional
- disabled by default
- configured locally in the app
- intended for local-network and self-hosted workflows

If MQTT is not enabled, the app behaves as a normal direct-control Bluetooth app.

## HTTP Gateway

Voltra Controller also includes an optional local HTTP gateway for people who want to:

- send commands with `curl`
- integrate with Node-RED or local scripts
- use the phone as a Bluetooth-to-LAN relay
- pull live state from another local service

The HTTP Gateway is:

- optional
- disabled by default
- authenticated with a local access key
- intended for your own LAN and self-hosted workflows

## Privacy And Permissions

Voltra Controller is built around direct local control.

### What It Does Not Require

- no user account
- no subscription
- no mandatory cloud sync
- no backend service just to control the device

### Permissions

- **Bluetooth / Nearby Devices**: required to scan for and connect to the Voltra
- **Notifications**: used for the foreground connection service while actively connected
- **Network**: only relevant for optional MQTT Sensor and HTTP Gateway use

Local preferences, last device info, and diagnostics remain on the device unless you explicitly export, share, or enable one of the optional network features.

## Safety

This is beta software controlling a resistance device. Please use common sense and keep early testing conservative.

Recommended approach:

- start with light resistance
- verify the selected mode on the Voltra itself
- test load and unload at low weight first
- keep unload easy to reach
- use exported diagnostics if the device and app disagree

If anything feels wrong, unload immediately and reconnect before continuing.

## Current Scope

Voltra Controller is focused on practical direct control first.

Current scope:

- one Android app
- one Voltra at a time
- direct Bluetooth control
- local settings and local diagnostics

Outside the current focus:

- cloud history systems
- account platforms
- OTA tooling
- multi-device tandem workflows
- polished public release distribution

## Beta Status

This project is now a working beta:

- the app is usable for real device control
- the supported core modes are live
- protocol coverage is still expanding
- polish and validation are still ongoing before a broader release

Play Store publication is now in progress as the beta settles into a more stable release shape.

## Project Docs

Additional project docs live in this repository:

- [Privacy Policy](./PRIVACY_POLICY.md)
- [HTTP Gateway](./HTTP_GATEWAY.md)
- [Play Store Beta Flow](./PLAYSTORE_BETA.md)
- [Wishlist Status](./WISHLIST_STATUS.md)
- [Capture Checklist](./CAPTURE_CHECKLIST.md)
- [Protocol Notes](./PROTOCOL_NOTES.md)
- [SDK Research Notes](./SDK_RESEARCH_NOTES.md)
