# Voltra Controller Privacy Policy

Effective date: April 16, 2026

Voltra Controller is a local-first Android application for connecting to and controlling a Voltra fitness device over Bluetooth.

This Privacy Policy explains what information the app handles, how that information is used, and when it may leave your device.

## Summary

Voltra Controller is designed to work without an account, subscription, or mandatory cloud service.

In normal use:

- the app connects to your Voltra over Bluetooth
- stores preferences and logs locally on your Android device
- does not require you to create a user account
- does not include advertising SDKs
- does not sell your personal information

Internet access is only needed for optional features you turn on yourself, such as MQTT publishing to your own broker.

## Information The App Handles

Depending on the features you use, Voltra Controller may handle the following categories of information:

### 1. Bluetooth And Device Information

- nearby Bluetooth device names and identifiers during scanning
- the connected Voltra device name and identifier
- device status such as connection state, battery level, and active workout mode

### 2. Workout And Control Data

- selected mode and resistance settings
- live readings such as reps, sets, phase, and other device telemetry
- mode-specific measurements such as isometric metrics when available from the device

### 3. App Preferences

- unit preference such as pounds or kilograms
- theme and display preferences
- last connected device information
- local control and safety-related preferences
- optional MQTT broker settings if you enable MQTT Sensor

### 4. Diagnostics And Logs

- Bluetooth connection events
- command attempts and results
- device discovery details
- raw and parsed protocol logs
- exported diagnostics you choose to share manually

## How The Information Is Used

Voltra Controller uses this information to:

- find and connect to your Voltra
- display live device status and workout information
- send control commands you initiate
- remember local app settings
- help troubleshoot connection and protocol issues
- optionally publish selected state data to an MQTT broker that you configure

## What Voltra Controller Does Not Do

Voltra Controller does not:

- require a user account
- require a subscription
- require cloud sync to control the device
- upload your workout data to a developer-operated backend as part of normal use
- sell personal information
- use advertising or third-party analytics SDKs inside the app

## When Data May Leave Your Device

Most app data stays on your Android device unless you choose otherwise.

Data may leave your device only in the following cases:

### 1. MQTT Sensor

If you enable MQTT Sensor, the app will send selected device and workout state data to the MQTT broker you configure.

That data is sent only to the broker address, port, and topic structure that you choose.

### 2. Manual Export Or Sharing

If you export or share logs or diagnostics, the exported file and its contents will leave the app and may be sent to another app, device, or person based on the share action you choose.

### 3. Android System Backup

Depending on your Android device settings, app data stored locally may be included in system-level device backups handled by Android or your device vendor.

## Permissions

Voltra Controller may request the following Android permissions:

### Bluetooth / Nearby Devices

Used to scan for, connect to, and communicate with your Voltra over Bluetooth Low Energy.

### Location On Older Android Versions

On older Android versions, location permission may be required by the operating system for Bluetooth scanning.
Voltra Controller does not use that permission to build a location history.

### Notifications

Used for the foreground service notification while the app maintains an active connection to the device.

### Internet

Used for optional MQTT Sensor functionality when you enable it.
Core Voltra control does not require internet access.

## Data Storage And Retention

Voltra Controller stores app data locally on your device.

This may include:

- preferences
- recent device information
- MQTT settings
- connection logs
- diagnostics captures

Data is generally kept until:

- you clear the app's storage
- you uninstall the app
- you remove or overwrite logs and settings inside the app

## Security

Voltra Controller is designed to minimize unnecessary data transfer by keeping core control and preferences local.

However, no software or storage system can guarantee absolute security.
If you enable MQTT or export logs, you are responsible for the security of your broker, network, storage destination, and sharing choices.

## Children's Privacy

Voltra Controller is not directed to children under 13 and is intended for use by people operating fitness equipment.

## Third-Party Services

This Privacy Policy covers Voltra Controller itself.
Your Android device, Google Play, your MQTT broker, and any share target you choose may have their own privacy practices and policies.

## Changes To This Policy

This Privacy Policy may be updated as the app evolves.
When the policy changes, the updated version should be published with a new effective date.

## Contact

Voltra Controller is maintained by Dylan Maniatakes / Technogizguy.

Project repository:

- https://github.com/dylanmaniatakes/Beyond-Power-Voltra-Android
