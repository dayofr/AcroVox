# AcroVox

Lecteur de podcasts Android, dans l'esprit d'AntennaPod. Suivi du projet dans Chantier (clé `PULSE`).

## Prérequis

- JDK 17 ou plus (`brew install openjdk@17`).
- Android SDK avec la plateforme 37.
- `local.properties` avec `sdk.dir=...` (non versionné).

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug
```
