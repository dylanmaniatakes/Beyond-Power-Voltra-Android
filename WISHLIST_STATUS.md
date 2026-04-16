# Voltra Controller Wishlist Status

This file tracks the wishlist without letting the experimental pieces get mixed into the core BLE control work. The rule for now: ship the local Weight Training control path first, then promote features from placeholder to active only after captures or Android-side tests prove the behavior.

Status key:

- Done: implemented enough to test in the Android app.
- In progress: partially implemented, still needs polish or hardware confirmation.
- Placeholder: visible or designed, but command writes are locked until captures confirm the protocol.
- Planned: practical, but not started yet.
- Future: larger project after core control is stable.

## App And Control Polish

| Feature | State | What is needed from you |
| --- | --- | --- |
| App name: Voltra Controller | Done | Install the next debug APK and confirm launcher/name look right. |
| Accent color picker | Done | Pick through the More or Home theme row and report any ugly combinations. |
| Top-right Weight Off toggle | Done | Test connected to the VOLTRA: unloaded should show Load, loaded should show Weight Off. Export diagnostics if the label disagrees with the device. |
| Weight Training button exits workout/goes home | In progress | Test whether it returns the VOLTRA to its main mode screen. Export diagnostics if it does not. |
| Digital dial style weight control | In progress | Set mode now keeps one pending drag value; retest slider drag then Set at 5, 10, 15, and 20 lb. |
| Tap number to dial in weight | Done | Test 5, 10, 15, and 200 lb entry. |
| Change weight +/- buttons | Done | Test whether the buttons feel better at 1, 5, 10, 15, or 20. |
| Customizable +/- increments by hold | Done | Long-press either +/- button to cycle 1, 5, 10, 15, and 20. The separate More-page picker was removed. |
| Instant weight apply mode | In progress | Hold Set to turn on blue Instant mode. Test gently at low weight and export diagnostics if it queues stale targets. |
| Default Set/Load mode preference | Done | The More page has a default instant-apply toggle; unload still forces the session back to Set mode. |
| kg support | In progress | UI can show kg locally, but BLE write encoding is still locked to lb. Need an iPad capture changing weights while the official app is set to kg. |
| Weight presets | Planned | No capture needed. Need a simple preset shape: name, weight, unit, chains/eccentric/curve later. |
| Saved device connection button | Done | Test reconnecting after app restart using Connect Saved Device. |

## Live Readings And Workout State

| Feature | State | What is needed from you |
| --- | --- | --- |
| VOLTRA battery sync | In progress | App now asks for `BMS_RSOC` during connect instead of waiting for a spontaneous update. Need one Android diagnostic after connect to confirm the Home gauge updates. |
| Rep counting | Done | User confirmed Android rep count and phase mirror the device. Keep watching for weird resets in longer workouts. |
| Device-backed set tracking UI | In progress | Android now parses sets from the same live telemetry frame as reps/phase. Retest a 3x4 workout and export diagnostics if the displayed set count disagrees with the Voltra. |
| Set counter matching device/iPad | In progress | `0x81 0x2B` telemetry byte 3 appears to be set count based on the 3 sets x 4 reps capture. More captures with visible notes will harden this. |
| Assist mode | In progress | Android now has a captured `FITNESS_ASSIST_MODE` toggle. Test On/Off in Weight Training and export diagnostics if the device UI or feel disagrees with the app. |
| Chains mode | In progress | Dynamic On/Off now follows the `BP_CHAINS_WEIGHT` value returned by BLE. Android caps Chains to `min(baseWeightLb, 200 - baseWeightLb)` and can edit while loaded. Need clean independent Chains/Inverse captures. |
| Inverse Chains mode | In progress | Separate dynamic On/Off now writes `FITNESS_INVERSE_CHAIN` and queues a BLE read-back, but recent testing says this parameter ACKs without obvious device effect. Need an official-app capture of inverse on/off. |
| Eccentric mode | In progress | Dynamic On/Off follows nonzero `BP_ECCENTRIC_WEIGHT`. The editor uses a positive magnitude slider with Add/Reduce direction buttons and can edit while loaded. Need clean load/unload captures at a few base weights to confirm positive overload behavior. |
| Adjustable custom curve | Placeholder | Need iPad captures selecting a curve, editing one point, saving/applying, load/unload. |
| Workout logs/tracking | Planned | No BLE capture required for basic local logs. Need rep/set state to be reliable first. |
| Export saved workout history as CSV | Planned | Depends on local workout logs. Need desired columns once logs exist. |

## More Page And Integrations

| Feature | State | What is needed from you |
| --- | --- | --- |
| MQTT Sensor | Planned | Need broker host/port/topic preferences later. Android LAN MQTT still needs the Android INTERNET permission, even for local network only. |
| Gateway mode over LAN | Future | Need core controls stable first. Later we need a command schema and security model. Also requires Android INTERNET permission. |
| Google voice/platform integration | Future | Need gateway or official Android integration design first. Likely requires internet/account-style platform plumbing. |
| In-app voice load/unload | Future | Need safety UX decisions. Voice recognition may need Android speech services and careful false-trigger protection. |
| Local automation integration path | Future | Start from protocol notes after Android control is stable. Need a host with reliable Bluetooth access if this moves beyond the Android app. |
| Google Health Connect API | Future | Need workout log fields first. Useful for workout duration, resistance, sets, reps, and calories only if we can derive them honestly. |
| Fitbit API access | Future | Need a concrete use case and auth flow decision. |
| Hevy workout integration | Future | Need API feasibility, auth decision, and a mapping from Hevy routines to Voltra presets. |

## Device And Multi-User

| Feature | State | What is needed from you |
| --- | --- | --- |
| Fully local user profiles | Planned | Need profile fields you care about: name, body weight, units, default presets, MQTT identity. |
| Paired companion support for 2 VOLTRAs | Future | Need single VOLTRA rock solid first. Later we need paired-device official captures and two-device Android hardware tests. |
| Android watch support | Future | Need phone app core stable. Later we can build a Wear OS companion with weight set and load/unload; crown input is a good fit. |
| Android widget | Future | Need stable connection state and safe command model. Widgets are awkward for Bluetooth commands, so this should come after gateway/watch decisions. |
| Tablet UI | Planned | Need screenshots from your tablet once the phone layout is stable. |
| App config/user data backup | Planned | Need preference for local file only vs Google Drive vs OneDrive. Cloud backup options add account/provider work. |
| Bootup intro selection/custom logo | Future | Need official iPad captures of selecting boot intros. If uploads are used, we need file format, size limits, transfer characteristic, and checksum. |

## Capture Queue

The existing detailed capture plan lives in `CAPTURE_CHECKLIST.md`. Highest-value next captures:

1. Rep count validation: Weight Training at 5 lb, load, exactly 1 rep, unload, export. Repeat for 2, 3, and 5 reps.
2. Exit workout: connect, enter Weight Training, load/unload once, tap the official app control that exits to device home, export.
3. kg mode: switch official app to kg, set the minimum, a mid value, and near max, export.
4. Chains and Inverse Chains: clean regular chains values, then inverse chains off/on, with no Eccentric changes mixed in.
5. Eccentric: clean off/on, small/mid/near-floor reduction values, load/unload after each.
6. Set counter: complete several sets and note the visible set number after each set, especially if the count ever resets.
7. Custom curve: select an existing curve, edit one point, save/apply, load/unload.
8. Resistance Band, Damper, Isokinetic, and Rowing: one short clean capture per mode before we add active buttons.
9. Battery and safety: naturally low battery, lock/child-lock states if easy and safe, disconnect/reconnect during a normal session.

For every capture, include a short text note with exact button presses and visible device/app state. The notes matter as much as the hex.
