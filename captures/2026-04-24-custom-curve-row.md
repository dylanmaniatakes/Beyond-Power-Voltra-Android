# 2026-04-24 Custom Curve and Row Mode

## Source Artifacts

- Android diagnostic: `/Users/ticnitsi/Downloads/text-0 2.txt`
- iPad sysdiagnose: `/Users/ticnitsi/Documents/BeyondPower-Port/Ipad Captures/sysdiagnose_2026.04.24_13-21-43-0400_iPhone-OS_iPad_22F76.tar.gz`
- iPad screen recording: `/Users/ticnitsi/Documents/BeyondPower-Port/ipad videos/ScreenRecording_04-24-2026 13-20-57_1.MP4`
- 50 m Row screen recording: `/Users/ticnitsi/Documents/BeyondPower-Port/ipad videos/ScreenRecording_04-24-2026 18-13-34_1.MP4`
- 50 m Row sysdiagnose: `/Users/ticnitsi/Documents/BeyondPower-Port/Ipad Captures/sysdiagnose_2026.04.24_18-08-23-0400_iPhone-OS_iPad_22F76.tar.gz`

## Custom Curve

- Android diagnostics confirmed the captured Custom Curve enter sequence reaches `Workout mode: Custom Curve, Ready`.
- The working flow is:
  1. `0x11 0x5183 EP_FITNESS_DATA_NOTIFY_SUBSCRIBE = F5 7B 65 00`
  2. `0xAF` bulk subscribe list
  3. `0x11 0x5182 EP_FITNESS_DATA_NOTIFY_HZ = 0x28`
  4. `0xAA 0x06` curve payload
  5. `0x11 0x4FB0 FITNESS_WORKOUT_STATE = 6`
  6. refresh mode feature state
- The `0xAA 0x06` curve payload has a 23-byte header followed by six little-endian `(x,y)` float32 graph samples split around a `0x0D` separator byte.
- Header bytes `4..5` appear to be range of motion in tenths of an inch; the captured value is `0x0492 = 117.0 in`, matching the iPad range-of-motion slider family.
- Android now preserves the UI's 118 in slider ceiling but caps the AA06 wire value to `0x0492 = 117.0 in`, because that is the highest captured iPad preset value and the hardware drifted at the uncapped high end.
- Header bytes `6..9` now decode as the iPad resistance range: `max16 + max8 + min8`. The focused capture default `64 00 64 05` is `5..100 lb`, and the iPad UI can set a low range such as `5..25 lb`.
- Android still mirrors the selected max to `0x3E86 BP_BASE_WEIGHT` and `0x5314 EP_MAX_ALLOWED_FORCE`, but those simple params did not enforce the Custom Curve limiter by themselves during hardware testing.
- The VOLTRA graph exposes four editable anchors; Android interpolates those four anchors into the six wire-format `(x,y)` samples.
- Default editable anchors: `0.0`, `0.24696325`, `0.5802966`, `1.0`, which expands back to the captured iPad default wire curve.
- Loading the mode uses the shared cable-read/load/refresh path: read `0x506A` and `0x3E82`, write `0x3E89=5`, then send `0xAA 13 01`.
- Live Custom Curve force is exposed in `0xB4` packets as mirrored first/last little-endian words in tenths of pounds. `0xAA 0x80` status packets also carry a Custom Curve force snapshot at payload offset 5 when payload starts `80 25 06 00`.

## Row Mode

- The iPad UI shows a Rowing Distance picker with `Just Row`, `50m`, `100m`, `500m`, `1000m`, `2000m`, and `5000m` options.
- The live screen shows total time, total distance meters, pace per 500m, stroke rate, average pace, kcal, projected finish, and splits.
- User-reported setting for this capture: resistance `4`.
- Correction after re-reading the raw sysdiagnose: the bundled PacketLogger files are stale Bluetooth history. `summaries/HCI.log` selected old April 18 PacketLogger files, and the VOLTRA frames inside those files end on April 17 with the known Isometric Test sequence.
- The Rowing screen recording is valid UI evidence, but this archive does not contain a fresh Row BLE entry capture.
- The earlier `0x4FB0=8` attribution was therefore unsafe. Android must not use the Isometric/Test entry path for Row.
- Live Android testing confirmed native Row entry/state as `0x4FB0=3` with `BP_SET_FITNESS_MODE=21`; Android now starts Row by writing the Row workout state, then refreshing the stream.
- Live Android testing after the passive attach change exposed the actual Row state tuple:
  - `APP_CUR_SCR_ID=62`
  - `FITNESS_ONGOING_UI=771`
  - `FITNESS_WORKOUT_STATE=3`
  - `BP_SET_FITNESS_MODE=21`
- `text-0 7.txt` confirmed manually opening Just Row on the VOLTRA lands on the same tuple and starts zeroed `AA95` packets even without a rep.
- `text-0 8.txt` showed the earlier candidate `EP_SCR_SWITCH=03 3E 00 01` could touch the Row UI, but the unit remained in the idle row shape: `AA95` packets were all zero and the state fell back to `APP_CUR_SCR_ID=61`, `FITNESS_ONGOING_UI=259`, `FITNESS_MODE=4`.
- `sysdiagnose_2026.04.24_18-30-10-0400_iPhone-OS_iPad_22F76` is a fresh iPad 50 m Row capture. It shows the captured row start action as `0x5165 EP_SCR_SWITCH = 06 3E 00 01`; the VOLTRA then reports `BP_SET_FITNESS_MODE=21`, `APP_CUR_SCR_ID=62`, `FITNESS_ONGOING_UI=771`, and begins `AA95` row telemetry. Because the source capture is 50 m, this is not yet proof of the Just Row selector itself.
- Live Android testing with hardcoded `06 3E 00 01` confirmed that every app option launched the VOLTRA's 50 m preset. Android now treats the first byte as an option action code instead of a generic start action. A later Android test showed `04 3E 00 01` leaves Just Row in the ready/menu state, so Just Row is testing the missing `05`, captured 50 m is `06`, and the remaining presets increment as `07..0B`.
- Live Row summaries use `cmd=0xAA` payloads beginning `95 25`. The best current mapping is status/metadata at byte `2`, pace per 500m at bytes `11..14` in centiseconds, elapsed time at bytes `15..18` in milliseconds, stroke rate at bytes `23..26` in centi-SPM, and displayed distance meters at bytes `35..38`.
- The live Row metric stream is `cmd=0xAA` payload family `95 25 ...` (39 payload bytes). Current mapping from `text-0 4.txt`: byte `2` is rounded distance meters, bytes `3..6` are a high-resolution distance candidate in centimeters, bytes `11..14` are current pace in centiseconds per 500m, bytes `15..18` are elapsed milliseconds, and bytes `23..26` are stroke rate in centi-SPM.
- The 18:13 screen recording shows the iPad 50 m flow and finish summary, but the paired 18:08 sysdiagnose still did not contain fresh Row BLE. Its HCI summary selected only April 18 PacketLogger files, and the indexed packets had no `AA95` row summary frames or row-entry writes. Treat this artifact as UI/metric evidence, not protocol-write evidence.
- The iPad preset split labels from the 18:13 video are:
  - `50m`: `10 x 5`
  - `100m`: `20 x 5`
  - `500m`: `100 x 5`
  - `1000m`: `200 x 5`
  - `2000m`: `500 x 4`
  - `5000m`: `1000 x 5`
- The 50 m result screen in that video showed `50 m`, `00:17.5`, `02:55.0/500m`, `51 spm`, `15` strokes, `1 kcal`, resistance level `4`, simulated wear level `8`, and cable length `0 cm`.
- Android now passes the selected row distance into the BLE start action and lets the VOLTRA promote itself to `BP_SET_FITNESS_MODE=21`, matching the iPad capture. It repeats the same selected action in delayed reassertions if the VOLTRA has not reached the live tuple.
- Candidate Row settings from `paramInfo.csv`:
  - `0x53A7 FITNESS_ROWING_DAMPER_RATIO_IDX`: Row resistance selector, shown as `1..10` in the app and written as zero-based `0..9`.
  - `0x53AE EP_ROW_CHAIN_GEAR`: simulated wear selector, shown as `1..10` in the app and written as zero-based `0..9`.
  - iPad defaults `resistance=4` and `simulated wear=8` line up with readback/cross-reference values `03` and `07`.
