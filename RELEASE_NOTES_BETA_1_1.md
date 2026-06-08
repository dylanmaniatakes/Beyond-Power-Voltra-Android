# Beta 1.1 Release Notes

Beta 1.1 adds the first implementation of VOLTRA Direct Load from the Android app.

## What's New

- Added Direct Load for supported workout modes, matching the captured iPad behavior where holding the main load control starts loading from a distance.
- Long-pressing the large weight value in Weight or Resistance mode now triggers Direct Load while a normal tap still opens the weight dial.
- The existing hold-to-load control now uses the Direct Load flow so it can be used as a clear fallback gesture.
- Added the Direct Load BLE command sequence recovered from the capture, including the AA12 trigger, status reads for 538D/53C7/53C8/53C9, and the repeated AA13 refresh stream.

## Improved

- The app now recognizes VOLTRA Direct Load fitness states 0x26 and 0x27 as engaged load states.
- Direct Load keeps the vendor state refresh stream active during the loading window so the app continues receiving status updates while the VOLTRA ramps in.

## Notes

Direct Load is capture-driven and should be tested conservatively. Start with low weight, keep clear of the cable path, and confirm that the VOLTRA behaves as expected before using heavier loads.
