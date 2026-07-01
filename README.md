# POSA

Portable Offline Survival Assistant, built by Unplugged AI.

POSA is a free, open-source, Android-first survival companion for hikers, campers, preppers, overlanders, CERT-style users, and other people who need practical offline field tools without relying on cloud services or cell signal.

The goal is not to build a generic survival chatbot. POSA should feel like a field instrument: fast, local-first, source-aware, and useful even when no AI model, account, backend, or internet connection is available.

## Status

This repository is at Phase 3: packs and guide cards.

A native Kotlin Android app shell is present under `app/`. It launches POSA with four top-level Compose tabs: Map, Tools, Guide, and Packs. Room-backed local storage covers waypoints, breadcrumbs, field notes, checklists, gear/tools, packs, guide cards, and provenance metadata. A bundled starter guide pack now installs from local app assets, parses a pack manifest plus Markdown card front matter, shows source/provenance metadata, and supports local keyword search. Mapsforge rendering, AI, accounts, analytics, and downloads are intentionally deferred to later roadmap phases.

## Current Product Decisions

- Platform: Android first.
- App name: POSA.
- Repo name: `portable-offline-survival-assistant`.
- Default Android package name: `ai.unplugged.posa`.
- License: Apache-2.0 for the app core.
- Content licensing: separate per pack, explicit in every pack manifest.
- Repo visibility: public from day one.
- Maps: Mapsforge-first offline OpenStreetMap strategy.
- Medical content: no treatment guidance in v0; first-aid kit checklists only until sourced and reviewed content exists.
- Telemetry: no analytics by default.

## v0 Focus

The first runnable prototype should include:

- Four top-level areas: Map, Tools, Guide, Packs.
- Offline Maps foundation using a Mapsforge-compatible approach.
- GPS coordinates, waypoints, breadcrumbs, distance, and bearing.
- Local guide cards loaded from bundled content packs.
- Visible provenance for guide content.
- Local checklists and field notes.
- User gear/tool inventory that can later become context for guided answers.
- Fully local storage using SQLite/Room.

## Non-Goals For v0

- No cloud backend.
- No user accounts.
- No required network dependency.
- No generic chatbot.
- No required local LLM.
- No Meshtastic, Bluetooth mesh, Wi-Fi Direct, LAN sync, or ATAK integration.
- No bulk map data bundled in the base app.
- No medical treatment instructions without licensed, reviewed sources.
- No hidden telemetry.

## Build Plan

Start with:

- [ROADMAP.md](ROADMAP.md)
- [docs/codex-session-plan.md](docs/codex-session-plan.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/map-data-strategy.md](docs/map-data-strategy.md)
- [docs/pack-schema.md](docs/pack-schema.md)
- [docs/content-guidelines.md](docs/content-guidelines.md)
- [docs/safety-policy.md](docs/safety-policy.md)
- [docs/build-in-public.md](docs/build-in-public.md)
- [docs/release-notes.md](docs/release-notes.md)

## Run The Android Shell

Prerequisites:

- Android Studio or a local JDK 17+.
- Android SDK platform 36.

If Java or the Android SDK is not on your shell path, point Gradle at the Android Studio toolchain first:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

From the repository root:

```sh
./gradlew :app:assembleDebug
```

Then open the project in Android Studio or install `app/build/outputs/apk/debug/app-debug.apk` on an Android device or emulator.

Run local JVM tests, including the Room repository CRUD and bundled pack loader/search coverage:

```sh
./gradlew :app:testDebugUnitTest
```

## Local Data Foundation

Phase 2 adds a Room database named `posa.db`, repository interfaces, Room repository implementations, and an explicit development seed path at `PosaDevelopmentSeed`. The seed data is fixture-only and marked as draft placeholder content; it is not user-facing survival guidance.

Room schema export is temporarily disabled because Room 2.8.4's KSP schema exporter currently hits a `kotlinx.serialization` `AbstractMethodError` when deserializing exported schema JSON during clean builds. Re-enable schema export after the Room/KSP dependency issue is resolved.

## Packs And Guide Cards

Phase 3 adds a bundled official draft pack at `app/src/main/assets/packs/wilderness-basics`. The local installer lives at `app/src/main/java/ai/unplugged/posa/data/pack/BundledPackInstaller.kt`, and the data-backed Guide/Packs UI lives at `app/src/main/java/ai/unplugged/posa/ui/GuideScreens.kt`. The app installs that pack locally on startup, loads six guide cards, stores per-card provenance, and renders Guide and Packs tabs from the local database. The guide content is draft source-aware synthesis and does not include medical treatment guidance.

## Safety Note

POSA is not a substitute for professional emergency, medical, legal, or rescue advice. The app must clearly show sources, review status, and uncertainty for field guidance. When emergency services are available, users should contact them.

## Attribution

OpenStreetMap data and any derived map packs must preserve required attribution and comply with applicable licenses, including ODbL where relevant.
