# Play Store Beta Flow

This project now supports two different release paths:

- `assembleRelease`
  - builds a local installable release APK
  - falls back to the debug signing key if upload signing is not configured
  - good for sideload alpha testing

- `bundlePlayRelease`
  - builds a Play-ready signed Android App Bundle (`.aab`)
  - requires a real upload keystore
  - intended for Play Console beta / internal / closed testing

## 1. Create An Upload Keystore

Create a private upload key that will be used only for Play uploads.

Example:

```bash
keytool -genkeypair \
  -v \
  -keystore voltra-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 9125
```

Keep this file somewhere safe and backed up.

## 2. Configure Signing

Copy:

`keystore.properties.example`

to:

`keystore.properties`

Then set:

- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

You can also provide the same values with environment variables:

- `VOLTRA_UPLOAD_STORE_FILE`
- `VOLTRA_UPLOAD_STORE_PASSWORD`
- `VOLTRA_UPLOAD_KEY_ALIAS`
- `VOLTRA_UPLOAD_KEY_PASSWORD`

## 3. Build The Play Bundle

From the repository root:

```bash
./gradlew bundlePlayRelease
```

If signing is configured correctly, the signed bundle will be created at:

`app/build/outputs/bundle/release/app-release.aab`

## 4. Upload To Play Console

Recommended flow:

1. Create the app in Play Console.
2. Enable Play App Signing.
3. Upload the generated `.aab` to the Internal Testing track first.
4. Verify install, Bluetooth permissions, and mode control on real hardware.
5. Promote to Closed Testing when the internal build looks stable.

## Versioning

Current package version:

- `versionCode = 105`
- `versionName = 1.3.2`

User-facing in-app label:

- `Alpha 1.3.2`

Increase `versionCode` for every new Play upload.
