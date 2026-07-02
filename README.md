# POSA

Portable Offline Survival Assistant, built by Unplugged AI.

POSA is a free, open-source, Android-first survival companion for hikers, campers, preppers, overlanders, CERT-style users, and other people who need practical offline field tools without relying on cloud services or cell signal.

The goal is not to build a generic survival chatbot. POSA should feel like a field instrument: fast, local-first, source-aware, and useful even when no AI model, account, backend, or internet connection is available.

## Status

This repository is at Phase 8: retrieval and RAG v1.

A native Kotlin Android app shell is present under `app/`. It launches POSA with four top-level Compose tabs: Map, Tools, Guide, and Packs. Room-backed local storage covers waypoints, breadcrumbs, field notes, checklists, gear/tools, packs, guide cards, and provenance metadata. A bundled starter guide pack installs from local app assets, parses a pack manifest plus Markdown card front matter, shows source/provenance metadata, and supports local keyword search. The Guide tab now includes an "Ask" retrieval mode that searches installed guide packs, returns cited source-card excerpts with confidence and provenance, folds in matching gear and saved map context, and responds "I do not know from installed sources" without generating unsupported medical or survival claims. The Guide tab also includes non-AI guided workflows for water, lost, shelter, fire, signaling, and battery conservation; workflow output is composed from installed guide cards, local checklist steps, gear inventory, and saved map context with missing-data warnings. The Tools tab supports local starter survival checklists, editable user checklists and checklist items, gear/tool inventory with have/missing state, and timestamped local field notes with optional location, waypoint, checklist, guide-card, and gear links. The Map tab embeds Mapsforge, renders a tiny bundled local Monaco test map fixture, imports supported user-selected Mapsforge `.map` files, requests Android location permission, displays current coordinates, saves current-location waypoints, shows waypoint details with distance and bearing, and records local breadcrumb trails. AI, accounts, analytics, sync, routing, and map downloads are intentionally deferred to later roadmap phases.

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

Run local JVM tests, including Room repository CRUD, starter checklist seeding, field tools CRUD, and bundled pack loader/search coverage:

```sh
./gradlew :app:testDebugUnitTest
```

## Local Data Foundation

Phase 2 adds a Room database named `posa.db`, repository interfaces, Room repository implementations, and an explicit development seed path at `PosaDevelopmentSeed`. The seed data is fixture-only and marked as draft placeholder content; it is not user-facing survival guidance.

Room schema export is enabled under `app/schemas`, and database migrations should include focused migration tests before new schema versions are added.

## Packs And Guide Cards

Phase 3 adds a bundled official draft pack at `app/src/main/assets/packs/wilderness-basics`. The local installer lives at `app/src/main/java/ai/unplugged/posa/data/pack/BundledPackInstaller.kt`, and the data-backed Guide/Packs UI lives at `app/src/main/java/ai/unplugged/posa/ui/GuideScreens.kt`. The app installs that pack locally on startup, loads six guide cards, stores per-card provenance, and renders Guide and Packs tabs from the local database. The guide content is draft source-aware synthesis and does not include medical treatment guidance.

## Checklists, Gear, And Field Notes

Phase 4 adds a data-backed Tools tab. On first local startup, POSA installs editable starter checklists for day hikes, emergency overnights, first-aid kit inventory, and vehicle field kits. Users can create, update, and delete local checklists and items, toggle item completion, maintain gear/tools with have or missing state, and write local field notes with title, body, created/updated timestamps, optional latitude/longitude, and optional links to existing waypoints, checklists, guide cards, and gear items. This data stays in local Room/SQLite storage; no cloud, account, analytics, sync, AI generation, or map implementation is added.

## Mapsforge Map Foundation

Phase 5 adds a data-backed Map tab using Mapsforge. POSA bundles `app/src/main/assets/maps/monaco.map` as a tiny local rendering fixture from Mapsforge's public test map source so early development can verify offline map rendering without bundling a large production map area. The app shows required OpenStreetMap attribution in the map UI, displays live device coordinates after Android location permission is granted, saves waypoints from the current location, calculates distance and bearing from the current fix to saved waypoints, and starts/stops local breadcrumb trail recording.

Phase 6 adds user-loaded map areas. The Map tab can import Mapsforge `.map` files through Android's storage picker, validates that the selected file is readable by Mapsforge, copies it into app-private storage, stores installed map metadata locally, and lets users enable, disable, or delete installed areas. Enabled user maps render offline without network access; when no user map is enabled, the tiny bundled Monaco fixture remains available for renderer checks.

Supported development/test map files can come from the Mapsforge public download index, such as `https://download.mapsforge.org/maps/v5/`. Use small regional files for testing. Do not bulk-download map tiles from public OpenStreetMap tile servers, and do not bundle large production map datasets in the base app. Routing, bulk map downloads, cloud sync, accounts, analytics, and AI generation remain intentionally unimplemented.

## Guided Workflows

Phase 7 adds deterministic guided workflows inside the Guide tab. Users can select water, lost, shelter, fire, signaling, or battery workflows. Each workflow shows source-backed bullets from installed guide cards, matching checklist steps, matching gear inventory with have/missing state, saved map context such as enabled map areas, waypoints, and active breadcrumbs, plus clear warnings when a local data category is missing. These workflows do not generate new survival advice and keep source links visible.

## Retrieval and RAG v1

Phase 8 adds source-grounded retrieval before any optional model generation. The Guide tab's "Ask" mode lets users type a question and search across installed guide packs. Results are ranked cited source-card excerpts, each shown with a confidence level and full provenance (source, citation, URL, review status). Where relevant, retrieval also surfaces matching gear inventory (have/missing) and saved map context (enabled map areas, waypoints, active breadcrumbs). When no installed source matches, POSA answers "I do not know from installed sources" rather than generating an unsupported claim. No generated medical treatment advice is produced; only installed-source excerpts are shown.

## Safety Note

POSA is not a substitute for professional emergency, medical, legal, or rescue advice. The app must clearly show sources, review status, and uncertainty for field guidance. When emergency services are available, users should contact them.

## Attribution

OpenStreetMap data and any derived map packs must preserve required attribution and comply with applicable licenses, including ODbL where relevant. The bundled Monaco Mapsforge fixture is used only as a small local rendering test fixture and is attributed in-app.
