# Voltra Controller

Voltra Controller is an accountless Android control and diagnostic app for a single Voltra.

Copyright (c) 2026 Technogizguy.

The first milestone is intentionally conservative:

- no account, cloud sync, subscriptions, or backend API calls
- no `INTERNET` permission
- BLE scan, connect, GATT discovery, notification capture, and diagnostics export
- guarded control APIs for strength mode, target load, load, and unload
- force-changing writes disabled until a real VOLTRA confirms the characteristic mapping, frame envelope, checksum, and command ids

The app uses the recovered iOS analysis and the bundled `paramInfo.csv` as a protocol seed. It does not treat string names from the decompiled app as proof that a command is safe to send.

Current roadmap and capture needs are tracked in `WISHLIST_STATUS.md` and `CAPTURE_CHECKLIST.md`.
