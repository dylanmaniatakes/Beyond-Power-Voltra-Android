# VOLTRA Capture Checklist

This is the running list of high-value captures for filling in the local Android protocol safely.

For every capture, record the exact steps in a small text note, keep the VOLTRA close to the iPad/phone, and export Android diagnostics immediately after the run. If using the official iPad app, grab the sysdiagnose within a minute or two of the action.

When reporting an issue or beta finding, include the app version, Android device/OS, visible VOLTRA firmware if available, exact mode and settings, expected result, actual result, exported diagnostics, and `/v1/state` or MQTT snapshots if integrations were involved.

## Highest Priority

1. Rep counter validation
   - Status: Android rep count and phase are confirmed by user testing to mirror the VOLTRA display.
   - Still useful later: one longer workout export with normal cadence, pauses, and unload/reload transitions.
   - Note whether Android's Sets/Reps/Phase card matches the visible Voltra counters.

2. Chains mode
   - Status: direct Chains weight writes are confirmed, and Android diagnostics now show the dynamic cap is `min(baseWeightLb, 200 - baseWeightLb)`.
   - Status note: Android `text-0 28.txt` showed `BP_CHAINS_WEIGHT` writes echo reliably, while `FITNESS_INVERSE_CHAIN` writes ACK but did not appear in the state stream.
   - Weight Training, 10 lb, 100 lb, and 172 lb.
   - Still needed: one clean capture toggling Chains off/on with no other menu changes.
   - Still needed: normal chains load/unload at low, mid, and near-limit values for each base weight.
   - Still needed: inverse chains off/on as a separate capture, load/unload once after each setting.
   - Still needed: any separate inverse-chain amount control, if the official app/device exposes one.

3. Eccentric mode
   - Status: direct Eccentric weight writes are confirmed for 0, -15, and -20 lb; Android now exposes a signed `-baseWeightLb..+baseWeightLb` range so positive overload can be tested.
   - Weight Training, 10 lb, 100 lb, and 172 lb.
   - Still needed: one clean capture toggling Eccentric off/on with no other menu changes.
   - Still needed: negative, zero, and positive eccentric values with load/unload once after each setting.
   - Note whether the device UI shows the same signed value as the iPad.

4. Custom curve
   - Open Custom Curve.
   - Select an existing curve, adjust one point, save/apply, then return to Weight Training.
   - Create a simple new curve if the app allows it.
   - Capture load/unload after applying the curve.

## Weight Training Feature Sheet

5. Auto Unload
   - Set Off, time-based, and rep-count based values if available.
   - Load, trigger the rule, then export.

6. Quick Adjust
   - Set 5 lb, 10 lb, and Off.
   - Pull the cable gesture once if safe, then export.

7. Resistance Experience
   - Status: `0x52CA` is now confirmed as the shared Standard/Intense toggle, and Android can drive it in Weight Training, Resistance Band, and Damper.
   - Still useful: one clean Resistance Band-only capture toggling Standard/Intense so we can confirm there is no extra profile-specific companion param.
   - Load/unload once per option.

8. Inverse Chains Amount
   - Status: Android can toggle/read back the `FITNESS_INVERSE_CHAIN` enabled flag, but the independent inverse amount write is still unknown.
   - In the official app or on the device screen, set base weight to 50 lb, set regular Chains to 0, set Inverse Chains to 10 lb, then export.
   - Set Inverse Chains to 25 lb, then 0 lb, without changing regular Chains.
   - Repeat once with regular Chains already nonzero so we can prove the two amounts are independent.

9. Assist
   - Toggle Off/On and capture any level choices.
   - Load/unload once per option.

10. Cable Length
   - Status: the `2026.04.15_15-35-05` iPad capture confirms trigger-adjusted cable movement updates `BP_RUNTIME_POSITION_CM` and saved cable offset updates `MC_DEFAULT_OFFLEN_CM`.
   - Status: the official entry write is `EP_SCR_SWITCH = 00 10 00 01`, and the official close/return write later in the same flow is `EP_SCR_SWITCH = 00 00 00 02`.
   - Test Android's Cable Length button in Weight Training ready, Weight Training loaded, Resistance Band ready, and Resistance Band loaded.
   - Expected Android behavior: the button should only open the VOLTRA cable-length screen/trigger flow, not pretend to set a numeric cable length directly.
   - Set 0 inch, a short fixed length, and a pulled-to-adjust length.
   - Export after each confirmed setting.

11. Velocity Target
    - Toggle Off/On and capture one target value.

## Other Modes

12. Resistance Band
    - Status: Android `text-0 30.txt` confirms `FITNESS_WORKOUT_STATE=2` when entering Resistance Band from the device screen.
    - Status: the `2026.04.15_15-35-05` iPad capture confirms `RESISTANCE_BAND_MAX_FORCE`, `BP_SET_FITNESS_MODE=5` load/start, and cable-trigger offset updates.
    - Status: Android `text-0 34.txt` shows the device floor is `15 lb`, so Android now clamps Resistance Band force to `15..200 lb`.
    - Status: Android now reuses the main control card for Resistance Band force, load/unload, and cable-length trigger so it behaves like the Weight Training screen with a different profile.
    - Still needed: one clean capture setting Resistance Band force to 50, 70, 100, then 70 again without opening other menus, because the latest capture described 70 lb but the clean later write was `0x5362=100`.
    - Enter mode, set minimum force, medium force, and max safe force.
    - Capture inverse mode if present.
    - Capture ROM/cable-length setup separately from force/curve setup.
    - Load/unload or start/finish once per setting.
    - Make this a clean capture with no Weight Training feature changes in the same run.

13. Damper
    - Enter mode and isolate just Damper.
    - Change the Damper value through a few clear steps such as `1 -> 3 -> 5 -> 10`.
    - Pull the cable briefly or load/unload once after each Damper change.
    - If the mode exposes metrics or another submenu, capture that separately.

14. Isometric
    - Status: Android now knows Isometric Test enters with `FITNESS_WORKOUT_STATE=8`, loads from the ready screen, and can render a dedicated realtime graph with locally derived peak / RFD / impulse metrics.
    - Still needed: one clean run where the official app changes any Isometric setting that is actually user-editable, plus a short start/finish hold.
    - Still needed: a capture of any metrics-type toggle or body-weight entry if the official app exposes one.

Custom Curve
    - Status: create/apply traffic is now clearly present, but it is using a separate `cmd=0xAA` vendor message family rather than the usual single-parameter writes.
    - Still needed: one clean capture creating a very small curve with only one point changed, then saving it.
    - Still needed: one clean capture opening an existing saved curve, applying it, and then exiting without editing it.
    - Still needed: if the official app can delete or rename curves, capture those separately from create/apply.

15. Isokinetic
    - Status: Android now has confirmed writes for enter mode, main target speed (`0x5350`), submenu selection, eccentric speed limit, constant resistance, and max eccentric load.
    - Still needed: a clean `1.5 m/s` main-card capture so we can verify a mid-range point between `1.0` and `2.0`.
    - Still needed: a short capture of the official app exiting Isokinetic cleanly so Android can mirror that finish flow more precisely.
    - Enter mode, set a low target speed and a higher target speed such as `0.5 -> 1.0 -> 1.5 -> 2.0 m/s`.
    - Capture eccentric options inside isokinetic separately.

16. Rowing
    - Enter rowing mode, change damper/drag settings, start/finish a short row.
    - Status: the stock Android bundle exposes `setCardioActivityModeForDevice` and `cardioActivityModeIndex`, but the numeric Row/Ski selector frame has not been recovered from the Hermes bundle yet.
    - Still needed: in the stock app Cardio menu, switch Row -> Ski -> Row before pressing Start, then export diagnostics so Android can compare selector writes around `EP_SCR_SWITCH`, cardio activity mode/index, and any unknown single-parameter writes.
    - Android should keep Ski start capture-only until this selector write is observed; do not reuse Row start as a Ski command.
    - Skip PM5 pairing unless intentionally testing PM5 later.

17. Sled Pull beta
    - Status: the 2026-06-08 decompiled Android APK from `/Users/ticnitsi/Downloads/Beyond+ (1).apk_Decompiler.com.zip` does not contain Sled Pull labels, enums, or command names. It only shows Rowing/Skiing cardio support.
    - After joining the beta, capture the stock app entering Sled Pull from a cold connect, changing any visible sled-specific settings, starting/loading, finishing/unloading, and exiting back to another mode.
    - Export Android diagnostics immediately after the run and note the exact stock app build, firmware version, visible mode label, and whether the feature arrived through a new APK or an in-app/CodePush update.
    - High-value fields to compare before and after entry: `FITNESS_WORKOUT_STATE` (`0x4FB0`), `BP_SET_FITNESS_MODE` (`0x3E89`), `APP_CUR_SCR_ID`, `FITNESS_ONGOING_UI`, any cardio activity mode/index field, `EP_SCR_SWITCH` (`0x5165`), and any new `0xAA` telemetry family.
    - Keep Android Sled Pull as capture-only until the selector, load path, and telemetry stream are observed; do not guess from Rowing or Skiing.

## Safety And State

18. Lock and child lock
    - Capture lock off/on if easy to trigger safely.
    - Try no load commands while locked only if the official app does it safely.

19. Battery state
    - Capture a normal battery report and a lower battery report if it naturally occurs.
    - Do not intentionally deep-drain the device.

20. Device main menu
    - From the VOLTRA screen, choose Weight Training, Resistance Band, Damper, and settings.
    - Capture Android passive logs while doing this so local state changes can be mapped without writes.

21. Disconnect/reconnect
    - Connect, handshake, load, unload, disconnect, reconnect, then export.
    - This catches sequence reset and stale state bugs.

22. Device set counter
    - Weight Training, low safe weight.
    - Complete at least three sets with visible set numbers noted after each set.
    - Export immediately after the third set.
    - This is specifically to validate telemetry byte 3 as the device/iPad set counter.

23. User/profile slots beta
    - Status: the 2026-06-08 Android bundle contains user-slot protocol names including `fetch-current-user-slot`, `fetch-user-slot-detail`, `switch-current-user-slot`, `createDeleteUserSlotCommand`, and `parseSwitchUserThroughIdPayload`.
    - Status: the Hermes bundle exposes names and response errors, but the current extraction did not expose safe frame IDs/bytes for list, read, switch, create, or delete.
    - Connect cold with the stock app, open the user/profile switcher, list users, switch to another user if safe, then export diagnostics immediately.
    - Capture create/delete only if the stock app makes the action reversible and it is safe for the VOLTRA owner.
    - Keep Android profile switching capture-only until the exact request and response traffic is observed.
