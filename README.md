# GO NOW — WearOS

Standalone Wear OS companion for [Tallinn GO](https://github.com/kapakas-ux/Tallinn-GO). Shows real-time public transport departures from your nearest stops in Estonia.

## Features
- GPS-based nearest stop detection (500m radius)
- Real-time departure times via peatus.ee GraphQL API
- Up to 3 nearest stops with 5 departures each
- Color-coded times (green = now, yellow = ≤2 min)
- Pull to refresh

## Tech Stack
- Kotlin + Compose for Wear OS (Material3)
- OkHttp + Gson for networking
- Google Play Services Location
- Min SDK 30 (Wear OS 3+)

## Build
```
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
