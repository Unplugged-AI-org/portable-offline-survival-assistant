# Release Notes

## Phase 8 Complete - Retrieval and RAG v1

Implemented:

- Guide tab "Ask" mode with a query interface over installed guide packs
- ranked, cited source-card excerpts with matched terms and confidence (low/medium/high)
- "I do not know from installed sources" behavior when no installed source matches
- confidence and provenance UI (source, citation, URL, review status) on each result
- gear inventory (have/missing) and saved map context (enabled areas, waypoints, breadcrumbs) folded into retrieval context where relevant
- retrieval tests for cited excerpts, unknown-question handling, and gear/map context inclusion

Known limitation:

- Retrieval is deterministic keyword/provenance scoring over installed sources only; there is no generation, embeddings, semantic search, or remote data.
- No generated medical or survival advice is added beyond installed-source excerpts.
- Optional local model support and semantic search remain later-phase work.

## Phase 7 Complete - Guided Workflows

Implemented:

- Guide tab workflow mode for water, lost, shelter, fire, signaling, and battery conservation
- deterministic workflow composition from installed guide cards, local checklist steps, gear inventory, and saved map context
- source-backed guide-card bullets with links back to full card provenance
- have/missing gear context and open/done checklist context in each workflow
- missing-data warnings when source cards, checklist steps, gear, or location context are unavailable
- workflow composition tests for source matching, search-filter independence, and missing-data warnings

Known limitation:

- Workflows do not generate new advice, fetch remote data, route on maps, or infer current GPS outside the Map tab.

## Phase 6 Complete - User-Loaded Map Areas

Implemented:

- Android storage-picker import for user-selected Mapsforge `.map` files
- import validation for unsupported extensions, empty files, and unreadable Mapsforge map data
- local app-private copy of imported map files for offline reuse
- Room-backed installed map metadata
- enable/disable controls for installed map areas
- delete control that removes installed metadata and the copied map file
- Map tab rendering of the first enabled user-loaded area, falling back to the bundled Monaco fixture when none is enabled
- documentation for supported Mapsforge test map sources
- Room schema export restored with committed version 2 schema JSON
- v1-to-v2 migration test coverage
- Kotlin, KSP, Room, and kotlinx-serialization dependency alignment for clean schema-export builds

Verified:

- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew testDebugUnitTest`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew clean testDebugUnitTest`
- `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew clean testDebugUnitTest assembleDebug`

Known limitation:

- Routing, bulk map downloads, sync, accounts, analytics, cloud services, and AI generation remain intentionally unimplemented.
- The app still bundles only the tiny Monaco renderer fixture and does not include large production map datasets.

## Phase 5 Complete - Mapsforge Map Foundation

Implemented:

- Mapsforge Android dependency integration
- tiny bundled Monaco Mapsforge `.map` fixture for local offline rendering checks
- Map tab with embedded Mapsforge `MapView`
- Android fine/coarse location permission flow
- current GPS coordinate display
- save waypoint from current location
- waypoint list/details with distance and bearing from current fix
- breadcrumb trail start/stop recording with stored points
- visible OpenStreetMap attribution in map UI and docs

Verified:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

Known limitation:

- The bundled Monaco fixture is intentionally tiny and only verifies local rendering; Phase 6 is still responsible for user-loaded map imports and installed map metadata.
- Routing, bulk map downloads, sync, accounts, analytics, cloud services, and AI generation remain intentionally unimplemented.

## Phase 4 Complete - Checklists, Gear, and Field Notes

Implemented:

- editable Room-backed Tools tab for checklists, gear, and field notes
- first-run local starter checklist installer
- checklist item completion state
- gear/tool have or missing state
- timestamped field notes with optional latitude/longitude, waypoint, checklist, guide-card, and gear links
- focused checklist, gear, field note, and starter checklist tests

Verified:

- `./gradlew clean :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

Known limitation:

- Map file import, routing, sync, accounts, analytics, cloud services, and AI generation remain intentionally unimplemented.
- Room schema export was re-enabled in Phase 6 alongside migration coverage.

## Phase 3 Complete — Packs and Guide Cards

Implemented:

- bundled wilderness-basics starter pack
- pack manifest/front-matter parser
- local bundled pack installer
- guide card list/detail screens
- visible source/provenance display
- local guide-card keyword search
- data-backed Guide and Packs Compose screens

Verified:

- `./gradlew clean :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

Known limitation:

- Room schema export was re-enabled in Phase 6 alongside migration coverage.
