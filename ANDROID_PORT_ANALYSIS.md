# Beyond+ iOS App Static Analysis and Android Port Plan

## Scope

This workspace is an unpacked iOS app bundle, not the original Xcode source tree. The main `EPRefactor` file is a Mach-O arm64 executable, and there is no `.xcodeproj`, `Package.swift`, Swift source directory, Core Data model, or test suite in the bundle.

That changes the port strategy. This cannot be converted to Android by recompiling or translating files in place. The practical path is to rebuild an Android application from the recovered product architecture, API vocabulary, resources, and hardware protocol clues, then validate against real VOLTRA and Concept2 hardware.

The findings below come from static inspection of:

- `Info.plist`, app extensions, Watch app metadata, and StoreKit configuration.
- Embedded framework and resource bundle metadata.
- Swift type names and string constants retained in the stripped executable.
- Localized strings, asset catalogs, Siri intent definitions, and BLE parameter CSV data.

## High-Level Product Model

Beyond+ is a hardware companion and workout platform for Beyond Power VOLTRA devices, with Concept2 PM5 rowing support. It is not just a mobile UI around a backend.

The app appears to do six major jobs:

1. Device discovery, pairing, activation, and local control for VOLTRA over Bluetooth LE.
2. Workout execution for strength, isometric, isokinetic, damper, resistance band, custom curve, rowing, eccentric, chains, auto-load, auto-unload, and safety-check workflows.
3. Workout/session planning, including manually created sessions, drop sets, circuits, AI-generated sessions, exercise libraries, rest durations, and athlete/profile selection.
4. Workout history, summaries, analytics, CSV export, Apple Health sync, and Concept2 Logbook integration.
5. Firmware, logs, Wi-Fi provisioning, custom logo transfer, beta updates, and device diagnostics.
6. Account, subscription, content/video channels, support/chat, localization, and social/community entry points.

## App Identity and iOS Surface Area

- App display name: `Beyond+`.
- Bundle identifier: `com.beyondpower.ep`.
- Current bundle version found in this artifact: `2.0.3` with build `1.0`.
- Minimum iOS version: `15.0`.
- Device family: iPhone only in the main app bundle.
- UI style: forced dark mode.
- URL scheme: `beyondpower`.
- Deep-link strings include `beyondpower://session/` and `beyondpower://logbookAuthentication`.
- Main storyboard entry is a small bootstrap controller: `EPRefactor.ViewController`.
- The app has `AppDelegate`, `SceneDelegate`, `BPMainTabController`, and `BPMainTabViewModel`, so the storyboard likely hands off into programmatic UIKit navigation.
- It embeds a Watch app: `com.beyondpower.ep.watchkitapp`, companion app required, with watch workout/physical-therapy background modes.
- It embeds a notification service extension: `com.beyondpower.ep.NotificationExtension`, apparently only for Intercom rich push handling.
- It declares custom Siri intents for `LoadWeight` and `UnloadWeight`, each taking a dynamic `Device` object.

Important iOS capabilities and permissions:

- Bluetooth central mode.
- Local network access.
- Background fetch, background processing, remote notifications, audio, and Bluetooth background mode.
- HealthKit read/write.
- Camera for QR codes.
- Photo library read/add for avatars, startup logo, and saving training plans.
- Microphone for voice messages.
- Location when in use, apparently to access Wi-Fi information.
- Background task identifier: `com.beyondpower.ep.tcp.connect`.
- App Transport Security allows arbitrary loads.

## Major Modules Recovered From Type Names

The binary retains enough Swift metadata to reconstruct the module split:

- `EPRefactor`: app-level orchestration, view models, app delegate, scene delegate, WatchConnectivity glue, account/session/history/workout logic.
- `BPUIComponent`: UIKit component library with most screens and cells.
- `BPBLECommunicator`: BLE/device SDK-style layer for VOLTRA and PM5.
- `BPAPIManager`: backend API clients for Beyond user APIs, firmware APIs, resource/content APIs, Concept2 APIs, and secure network handling.

The architecture is strongly MVVM-ish around UIKit controllers:

- UI: `BPHomeController`, `BPMainTabController`, `BPDeviceListController`, `BPVOLTRADashboardController`, `SessionWorkoutController`, `WorkoutDetailController`, `HistoryControllerV3`, `BPSubscriptionController`, `C2LoginController`, many table/collection cells.
- App state and orchestration: `BPHomeViewModel`, `BPDeviceViewModel`, `BPWorkoutDataViewModel`, `WorkoutDetailViewModel`, `BPWorkoutDataHistoryViewModel`, `HistoryViewModelV2`, `BPUserWorkoutSessionsViewModel`, `BPSubscriptionViewModel`, `BPOTAViewModel`, `HealthKitViewModel`, `Concept2OAuthViewModel`.
- Device SDK: `BPBLECommunicateManager`, `BPBLECentralManager`, `BPBLEVOLTRAPeripheral`, `BPBLEPM5Peripheral`, `BPSDKWorkoutManager`, `BPSDKOTADataManager`, `BPSDKSubscriptionDataManager`, `BPSDKParamDataManager`, `BPSDKTransportManager`, `BPSDKActivateBizManager`.
- Protocol helpers: `BPVOLTRAPeripheralCommonHelper`, `BPVOLTRAPeripheralOTAHelper`, `BPVOLTRAPeripheralParamHelper`, `BPVOLTRAPeripheralWorkoutHelper`, `BPVOLTRAPeripheralActivateHelper`, `BPVOLTRAVersionFeatureManager`, `BPVOLTRAWeightUnitConvertManager`.

There is evidence of The Composable Architecture and related Point-Free libraries being statically linked, but most recovered product screens are UIKit controllers backed by view models rather than pure SwiftUI.

## Third-Party and Platform Dependencies

Detected dependencies and likely Android equivalents:

| iOS dependency or framework | Role | Android replacement |
| --- | --- | --- |
| CoreBluetooth | VOLTRA and PM5 BLE | Android BLE via Nordic BLE Library or RxAndroidBle |
| BackgroundTasks | TCP reconnect/background work | WorkManager, foreground service for active device work |
| WatchConnectivity | iPhone to Apple Watch sync | Wear OS Data Layer if Android watch support is required |
| HealthKit | Health/workout sync | Health Connect |
| StoreKit | subscriptions | Google Play Billing |
| AuthenticationServices | Sign in with Apple | Credential Manager plus Apple web OAuth if still required |
| UserNotifications + notification extension | push/rich push | FCM, notification service, Intercom Android SDK |
| Intercom 17.2.1 | support/chat/rich push | Intercom Android SDK |
| Alamofire | HTTP, uploads, downloads, websockets | Retrofit/OkHttp/Ktor |
| GCDAsyncSocket/GCDAsyncUdpSocket | TCP/UDP local device connection | OkHttp socket/WebSocket or raw Kotlin coroutines sockets |
| Kingfisher | image loading/cache | Coil |
| TOCropViewController | crop images | uCrop or custom Compose cropper |
| QRCode / QR scanner | QR generation/scanning | ML Kit Barcode Scanning / ZXing |
| Charts | analytics charts | Vico, MPAndroidChart, Compose charts, or custom Canvas |
| AVFoundation/AVKit | video playback | ExoPlayer / Media3 |
| Photos/PhotosUI | images, avatars, startup logo | Android Photo Picker / MediaStore |
| CoreLocation + SystemConfiguration | Wi-Fi info/provisioning support | Android Wi-Fi APIs, Nearby permissions, Location permission |
| CryptoKit/Security/Keychain | tokens, checksums, secure storage | Jetpack Security Crypto, Android Keystore |

## Backend and External Services

Recovered base URLs:

- Production API: `https://api.beyond-power.com`.
- Staging API: `https://api-stg2.beyond-power.com`.
- Firmware CDN/API: `https://fmw-aws.beyond-power.com`.
- Resource/content host: `https://res.beyond-power.com`.
- Concept2 production: `https://log.concept2.com`.
- Concept2 development: `https://log-dev.concept2.com`.
- Local device/TCP endpoints seen in strings: `http://192.168.11.25:11451` and `http://192.168.200.201:12443`.

Recovered API path vocabulary:

- Account/auth/profile: `sso/verify-token`, `sso/me/verify/refresh-token`, `me/email-verification`, `me/verification-code`, `verification/code`, `me/password-check`, `me/profile/unit-prefer`, `me/athlete/profile`, `me/athletes/profile`, `me/athlete-group`, `me/athlete-groups`.
- Sessions and AI generation: `me/custom-session`, `me/custom-session/v2`, `me/session/generate-template`, `me/session/residual-count`, `me/session/records`, `me/session/workout/details`.
- Workout data: `me/workout/details`, `me/history-info/v2`, `me/monthly-summary`, `me/monthly-summary/v2`, `me/summary/batch`, `me/batch-sync-actions`, `me/action/optional-types`, `me/optional-actions`.
- Exercise/action library: strings mention action types, optional actions, custom exercises, body part lists, and generated session templates.
- Content/resources: `resource/videos`, `resource/channels`, `resource/logo/meta-info`.
- Subscriptions/products: `subscribe/country`, `me/products/info`, `me/transactions`.
- Concept2: `oauth/authorize`, `record/login/and/bind`, `rowing/me/workout-configs`, `rowing/me/workouts`, `rowing/me/summary`, `users/me/results`.

Model names indicate JSON payloads for workout summaries, rowing splits, reps, session configs, athlete summaries, custom workout actions, subscription transactions, Intercom auth info, and monthly summaries.

## Account, Auth, and Subscription Behavior

The app supports:

- Email signup with verification codes.
- Password creation, password reset, password existence checks, and account deletion.
- Sign in with Apple.
- Beyond token persistence under strings such as `Beyond-Power-EP-Token`, `diskTokenKey`, `userToken`, and `c2TokenKey`.
- Token invalidation and refresh handling.
- Intercom login via an `IntercomAuthInfo` / `chatAccessToken` model.
- Concept2 OAuth with a `beyondpower://logbookAuthentication` callback.
- Subscription entitlement checks, country availability, product info, transactions, and redeem code flow.

StoreKit configuration contains two auto-renewing monthly subscription products:

- `com.beyondpower.ep.athleanX.monthly.v1`, `AthleanX Monthly`, $1.99, one-week free intro.
- `com.beyondpower.ep.beyond_exercise.monthly.v1`, `Beyond Channel Monthly`, $9.99, one-week free intro.

For Android, this needs a Google Play Billing entitlement mapping. Do not rely on iOS product IDs only; define server-side entitlements that can be backed by Apple or Google purchases.

## Device and BLE Layer

The BLE layer is the hardest and most important part of the Android port.

Detected VOLTRA concepts:

- Device types: VOLTRA, twin VOLTRA, Concept2 PM5.
- Connection states: available, connecting, connected, disconnected, connected by another app/device, rejected by VOLTRA, maximum connection count.
- Activation: fetch activation code, activate to VOLTRA, confirm activation, fetch activation state.
- Device metadata: name, BLE address, serial/SN code, firmware tag/version, product info, battery health, cable health, cable length unit, lock state, child lock, low battery, exception states.
- Pairing: master/slave BLE addresses, twin pairing/unpairing, twin timeout/failure/refusal, model/firmware compatibility checks.
- Safety: blocked operations during workouts, lock-state validation, child protection, safety check, cable extraction constraints, low-battery blocks.
- Commands: command queue, command timeout, pending command detection, write retry count, response buffer, handshake state.

Detected BLE UUIDs include many Concept2 PM5 UUIDs under the `CE0600xx-43E5-11E4-916C-0800200C9A66` family, plus additional app/device UUIDs:

- `CE060000-43E5-11E4-916C-0800200C9A66`
- `CE060010-43E5-11E4-916C-0800200C9A66`
- `CE060013-43E5-11E4-916C-0800200C9A66`
- `CE060014-43E5-11E4-916C-0800200C9A66`
- `CE060016-43E5-11E4-916C-0800200C9A66`
- `CE060017-43E5-11E4-916C-0800200C9A66`
- `CE060018-43E5-11E4-916C-0800200C9A66`
- `CE060020-43E5-11E4-916C-0800200C9A66`
- `CE060021-43E5-11E4-916C-0800200C9A66`
- `CE060022-43E5-11E4-916C-0800200C9A66`
- `CE060030-43E5-11E4-916C-0800200C9A66`
- `CE060031-43E5-11E4-916C-0800200C9A66`
- `CE060032-43E5-11E4-916C-0800200C9A66`
- `CE060033-43E5-11E4-916C-0800200C9A66`
- `CE060034-43E5-11E4-916C-0800200C9A66`
- `CE060035-43E5-11E4-916C-0800200C9A66`
- `CE060036-43E5-11E4-916C-0800200C9A66`
- `CE060037-43E5-11E4-916C-0800200C9A66`
- `CE060038-43E5-11E4-916C-0800200C9A66`
- `CE060039-43E5-11E4-916C-0800200C9A66`
- `CE06003A-43E5-11E4-916C-0800200C9A66`
- `CE06003B-43E5-11E4-916C-0800200C9A66`
- `CE06003D-43E5-11E4-916C-0800200C9A66`
- `CE06003E-43E5-11E4-916C-0800200C9A66`
- `CE06003F-43E5-11E4-916C-0800200C9A66`
- `CE060080-43E5-11E4-916C-0800200C9A66`
- `19DE84ED-0A69-482C-A8A6-C75CB5BB4389`
- `55CA1E52-7354-25DE-6AFC-B7DF1E8816AC`
- `E4DADA34-0867-8783-9F70-2CA29216C7E4`
- `a010891d-f50f-44f0-901f-9a2421a9e050`
- `ca94658c-0525-5046-e78b-5391b65f47ad`

The `BPBLECommunicator_BPBLECommunicator.bundle/paramInfo.csv` file is the best protocol artifact in the bundle. It contains 2,211 parameter definitions with IDs, names, descriptions, units, value types, byte lengths, defaults, min/max values, reboot requirements, category, writability, volatility, and submodule. Sample parameters include:

- `FITNESS_REST_TIME_OVERRIDE`
- `CABLE_STABLE_DUR_MS`
- `AUTO_UNLOAD_HOLDING_ST`
- `EP_MAX_CHAINS_PCT`
- `S_TYPE_LOAD_CELL`
- `MC_INERTIA_COEFFICIENT`
- `CHG_CURR`

The Android port should treat this CSV as a seed for a typed Kotlin parameter registry.

## VOLTRA Workout Protocol Concepts

The device SDK appears to expose separate managers for:

- Common commands: reboot, fetch device name, fetch product/SN data, BLE connection params, state fetches.
- Parameters: fetch parameter lists, fetch subscribed parameters, add/delete/modify subscriptions, notify rates.
- Workout control: set workout type, screen state, interval count, fetch current workout ID, summary, set summary, reps data, batch reps data, coworker/twin summaries.
- OTA: enter OTA mode, transfer pack slices, MD5 check, check upgrade state, upgrade subsystem, reboot, progress notifications.
- Activation: verification code and activation state.
- Transport/logos: prepare transfer logo, transfer logo, compression state, slot occupation/current slot, read log files.

Recovered workout modes and features:

- Weight training.
- Resistance band.
- Damper.
- Isokinetic.
- Isometric.
- Custom curve.
- Rowing.
- Eccentric.
- Chains.
- Auto-load.
- Auto-unload.
- Straight weight-on/off.
- Safety check.
- Twin/coworker mode.
- PM5 rowing integration.

Recovered metrics:

- Cable length/current length/start offset/range of motion.
- Base/gear/average weight.
- Force/resistance, power, peak power, velocity, peak velocity, target velocity.
- Reps, sets, rest duration, time under tension.
- Total volume, total work, pull volume, pull distance, pull duration.
- Rowing distance, split time, split calories, total calories, stroke count/rate, drag factor, pace, projected finish.
- Heart rate through HealthKit.

## Wi-Fi, TCP, Logs, and OTA

VOLTRA has a Wi-Fi AP/log retrieval path in addition to BLE.

Recovered concepts:

- Open/close VOLTRA Wi-Fi AP through BLE commands.
- Fetch current Wi-Fi info and sync SSID/password.
- UI for Wi-Fi configuration.
- TCP reconnection with background task identifier `com.beyondpower.ep.tcp.connect`.
- Errors for Wi-Fi state, open/close failure, timeout, and network reachability.
- Log file fetching, slot occupation/current slot, user file info/file/extension, compression state, `endReadingLog`.
- Firmware download, validation, MD5 check, pack transfer by slice, subsystem upgrade state/progress, reboot, reconnect timeout.

Android implementation likely needs:

- BLE control channel for AP setup.
- A foreground service while fetching logs or transferring firmware.
- OkHttp/raw socket client for local TCP.
- Strict retry, timeout, and progress reporting.
- Android 12+ nearby devices and Bluetooth permissions.
- Location permission only if SSID/BSSID access is still required.

## UI and Navigation Shape

The app has five primary tab assets:

- Home.
- Session.
- History.
- Athlete.
- Profile.

Important screens/controllers recovered:

- Home/dashboard: `BPHomeController`, `BPVOLTRADashboardController`, `BPVOLTRADashboardDetailController`, `BPDeviceListController`, `BPModifyVOLTRANameController`.
- Device setup: activation, QR scanner, permissions, Wi-Fi provisioning, OTA, firmware list, fetch logs, custom startup logo, warranty, safety check, shortcuts.
- Workouts: session detail, session workout, set detail, exercise list/detail, circuits, drop set, target controller, real-time VOLTRA data, rowing real-time data.
- History/analytics: history V2/V3, summary display V3, rowing summary, isometric summary, charts, export/share.
- Account: sign in, sign up, password reset, profile, athlete groups, language, unit sync, advanced settings, delete account, redeem code.
- Content: video welcome, content channel, video content, subscriptions.

Assets confirm most of these surfaces: VOLTRA icons, PM5 placeholder, rowing content, resistance mode icons, body part icons, tab bar icons, social icons, subscription icon, Health icon, Siri icon, QR image, startup-logo assets.

## Data Storage and Cache

No database schema is present in the app bundle. Static strings indicate these storage areas:

- Keychain wrapper: `BPKeyChainInterface`.
- Token keys: `Beyond-Power-EP-Token`, `diskTokenKey`, `c2TokenKey`.
- Device storage helper with a max VOLTRA count and PM5 storage.
- Cache names: `com.beyondpower.players.cache`, `com.beyondpower.logs.cache`, `com.beyondpower.videos.cache`, `com.beyondpower.videos.cache.v2`.
- Local history cache and summary dictionaries.
- Connection UUID storage.
- Kingfisher image cache.

Android equivalents:

- EncryptedSharedPreferences or DataStore backed by Android Keystore for tokens.
- Room for structured workout/session/history/device cache.
- Coil disk cache for images.
- App-specific files for logs, firmware packs, CSV exports, and generated share images.

## Localization

The bundle includes localized resources for:

- English.
- Simplified Chinese.
- German.
- Spanish.
- French.
- Italian.
- Japanese.
- Korean.

The English `Localizable.strings` contains product strings for the main app, while third-party bundles include their own localization files. Android should migrate these into `res/values-*/strings.xml` or Compose multiplatform resource files. Watch out for iOS format placeholders such as `%1$@` and `%1$lld`; these need Android-compatible placeholders such as `%1$s` and `%1$d`.

## Android Architecture Recommendation

Recommended baseline:

- Kotlin-only native Android app.
- Jetpack Compose for new UI, unless matching the existing UIKit screens pixel-for-pixel is critical.
- MVVM/MVI with Kotlin Flows.
- Hilt for dependency injection.
- Retrofit/OkHttp for APIs, downloads, uploads, and local sockets where appropriate.
- Room for local workout/session cache.
- DataStore + Jetpack Security for preferences and secrets.
- Nordic BLE Library or RxAndroidBle for VOLTRA/PM5 BLE.
- WorkManager for deferrable sync; foreground services for active BLE workout, OTA, log transfer, and long-running device operations.
- Health Connect for workout/heart-rate sync.
- Google Play Billing for subscriptions.
- FCM + Intercom Android SDK for push and support.
- Media3 ExoPlayer for videos.
- ML Kit or ZXing for QR scanning.

Do not build this as a WebView or simple React Native shell unless the BLE/foreground-service layer is native and treated as the core product. The risk in this port is hardware protocol fidelity and background execution, not basic UI rendering.

## Proposed Android Module Split

```text
app/
  MainActivity, navigation, Compose screens, push entry points

core-model/
  Domain models for devices, sessions, workouts, summaries, subscriptions

core-network/
  Retrofit/OkHttp, auth interceptor, token refresh, API DTOs

core-storage/
  Room, DataStore, encrypted token storage, file/cache managers

device-ble/
  Scanner, connection manager, command queue, UUID registry, VOLTRA protocol

device-voltra/
  Activation, params, workout control, OTA, logs, Wi-Fi provisioning

device-pm5/
  Concept2 PM5 BLE/CSAFE protocol and rowing data parser

feature-home/
feature-device/
feature-session/
feature-workout/
feature-history/
feature-profile/
feature-content/
feature-subscription/
feature-onboarding/

integration-health/
integration-intercom/
integration-billing/
integration-wear/
```

## Suggested Port Phases

### Phase 0: Missing Artifacts

Collect the artifacts that are not present in this bundle:

- Original iOS source repository if available.
- VOLTRA BLE protocol specification, including UUID-to-characteristic mapping, command frame format, CRC8/CRC16 algorithms, command IDs, response codes, and firmware compatibility matrix.
- PM5/CSAFE support requirements and expected firmware versions.
- Backend API documentation or OpenAPI schema.
- Subscription entitlement rules.
- Test hardware matrix: single VOLTRA, twin VOLTRA, VOLTRA in OTA mode, low battery, locked/child lock, PM5, poor BLE conditions.

### Phase 1: Android Foundation

Build app skeleton, navigation, theme, localization, API clients, token storage, and account flows.

Deliverables:

- Login/signup/password reset.
- Token refresh and logout.
- Profile and unit preference.
- Basic home/profile tabs.
- Backend integration tests against staging.

### Phase 2: BLE Discovery and Device State

Build the Android BLE stack before the workout UI.

Deliverables:

- Permission flow for Android 12+ `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
- Scan filters for VOLTRA and PM5.
- Connection/reconnection state machine.
- Service/characteristic discovery.
- Command queue with write timeout, retry, response parsing, and cancellation.
- Device info fetch: name, BLE address, firmware, battery, cable, activation state.

### Phase 3: VOLTRA Control

Implement the core VOLTRA commands and safety model.

Deliverables:

- Activation flow.
- Modify device name.
- Lock/child/low-battery safety validation.
- Workout mode switch with throttling.
- Weight on/off.
- Cable length and range-of-motion controls.
- Parameter registry generated from `paramInfo.csv`.
- Feature gating by firmware version.

### Phase 4: Workout Execution

Port workout/session flows after the control layer is stable.

Deliverables:

- Strength workout execution.
- Session planner and exercise list.
- Sets/reps/rest/drop set/circuit logic.
- Real-time data subscriptions.
- Workout summary fetch and local cache.
- CSV export.

### Phase 5: OTA, Logs, Wi-Fi, and Custom Logo

These should come after basic workouts because they carry higher bricking/data-loss risk.

Deliverables:

- Firmware list and download.
- Pack MD5 validation.
- OTA mode entry.
- Slice transfer and progress.
- Subsystem upgrade status.
- Reboot/reconnect flow.
- Wi-Fi AP open/close, TCP log fetch, log compression/read flow.
- Custom startup logo crop/transfer.

### Phase 6: Integrations

Deliverables:

- Concept2 OAuth and PM5 rowing.
- Health Connect sync.
- Google Play Billing subscription parity.
- Intercom support and rich push.
- Wear OS companion if required.
- AI session generation and video/content subscriptions.

## Biggest Android Risks

1. BLE protocol fidelity. The app has a sophisticated command queue, handshake, CRC, subscriptions, OTA, and summary-recovery flow. Reimplementing this from static names alone is risky.
2. Android background restrictions. Active workouts, BLE reconnection, OTA, and local TCP transfer will require foreground-service design and clear user-visible notifications.
3. Device safety. The app blocks many operations during workouts, low battery, lock/child lock, pairing, unpairing, OTA, and active cable extraction. Missing these rules can be dangerous.
4. Firmware feature gating. Many features are version-gated, such as OTA pack support, basic EP param fetches, safety checks, firmware tags, session units, and block-during-exercise behavior.
5. Twin VOLTRA state. Master/slave pairing, matching firmware/model checks, coworker summaries, and twin unsupported flows add complexity.
6. Health/subscription equivalence. HealthKit and StoreKit behavior do not map one-to-one to Health Connect and Google Play Billing.
7. Missing source. Without source or protocol docs, many details must be recovered from runtime behavior or device traffic.

## Recommended Next Steps

1. Obtain the original iOS source and private Swift packages if possible. This would change the effort from reverse-engineering-plus-rebuild to a normal native port.
2. Generate a formal protocol spec from `BPBLECommunicator`, starting with `paramInfo.csv`, BLE UUIDs, command frame layout, CRC implementation, and response-code mapping.
3. Build a small Android BLE spike that only scans, connects, discovers services, performs handshake, and fetches device info.
4. Build a backend API contract document from staging using the recovered path list and actual request/response captures.
5. Define the Android product scope for v1: account + one VOLTRA + basic strength workout + history sync is the most realistic first milestone.
6. Defer OTA, twin mode, PM5 rowing, AI sessions, and Wear OS until the BLE core is proven on multiple devices.

## Working Assumptions for an Android v1

- Native Kotlin is the best fit.
- Real hardware access is required early.
- The BLE/device layer should be built as a reusable SDK-style module, not buried in screens.
- The server should become the source of truth for subscriptions and user entitlements across Apple and Google.
- Android v1 should prefer reliable core hardware control over full feature parity on day one.

