# CAPJI Hardware Lab — `com.capji.hardwarelab`

تطبيق أندرويد احترافي لفحص العتاد (Kotlin • Jetpack Compose • Material 3 • MVVM) مع واجهة عربية RTL وإنجليزية.
A professional Android hardware diagnostics lab. All readings come from real Android APIs (no mocked data).

## Build
GitHub Actions (`.github/workflows/build.yml`) builds `app-debug.apk` on every push to `main`.
Download it from **Releases → latest** or from the run's **Artifacts**.

Local: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Firebase (optional)
Put your `app/google-services.json` in place to enable FCM (topic `all_users`) and Remote Config.
Without it, the plugin is not applied and the app builds and runs normally.

© 2026 CAPJI — Instagram axh.d. · TikTok @c1fq_7 · WhatsApp +967772678571
