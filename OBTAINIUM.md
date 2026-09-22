# AcroVox via Obtainium

AcroVox is not on the Play Store. The recommended way to install it
and get updates is [Obtainium](https://obtainium.imranr.dev/).

## Install

1. Install Obtainium (from its website or its GitHub release).
2. In Obtainium: **+ Add app**.
3. Paste the repository URL: `https://github.com/dayofr/AcroVox`.
4. The source is auto-detected (GitHub). Pick the `acrovox-v*.apk`
   file from the asset list.
5. Install.

## Updates

Each version is published as a GitHub release (`v0.1.0`, `v0.2.0`, …).
Obtainium detects the new release and offers the update,
as a notification or automatically depending on its setting.

## Notes

- Do not install the debug build (`com.acrovox.app.debug`): it is a
  separate app and won't get these updates.
- Updates require the same signature from one APK to the next. As long as
  the project signing key doesn't change, everything installs over the top.

## Maintainer note

- To cut a release: `git tag vX.Y.Z && git push origin vX.Y.Z`.
  The `release.yml` workflow builds the release APK and attaches it.
- Keep the same keystore (secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`). Without it, the APK comes out unsigned and
  updating over a signed version fails.
- The `versionCode` (`ACROVOX_VERSION_CODE`) must always increase.
