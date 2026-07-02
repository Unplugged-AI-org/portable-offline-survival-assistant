# POSA — Workstreams / Department Program Plan

This is the master program plan for POSA after roadmap phases 0–8. Instead of
working phase-by-phase, ongoing work is organized into parallel **departments**,
each run in its own focused session and its own branch/PR. This document is the
source of truth: update the "Current State Snapshot" as departments land work.

**Working model (agreed):**
- One session per department. This document seeds each session.
- Branch per department (e.g. `ui-architecture`, `maps-overlays`); one concern per PR.
- First department: **UI Architecture** — ✅ **landed** on branch `ui-architecture`
  via incremental ViewModel extraction (app built and tested after each step).
- Maps department overlay core — ✅ **landed** on branch `maps-overlays`, followed
  by long-press/tap polish on `maps-longpress-waypoint` and `fix-longpress-npe`.
  Next up: manual device/emulator verification, then parallelize the
  content/retrieval track (#3→#4, #5→#4).

---

## Current State Snapshot

**Solid / done (roadmap phases 0–8):**
- UI architecture: `PosaApp.kt` reduced from a 772-line monolith to a 338-line
  nav + scaffold + VM-wiring shell. Three `StateFlow`-backed ViewModels
  (`MapViewModel`, `ToolsViewModel`, `GuideViewModel`) own their content load,
  mutations, and UI selection state. `DestinationScreen` prop-drilling collapsed
  from 19 params to 5 (the three VMs + destination + padding), with per-tab
  binding composables. Cross-domain refresh isolated to two documented callbacks
  (`MapViewModel.onDataChanged`, `GuideViewModel.onGuidePackInstalled` →
  `ToolsViewModel.reload()`). 14 new ViewModel unit tests.
- Data layer: 11 Room entities, 9 DAOs, 9 repositories, mappers, 3 migrations
  (v1→v4), in-memory test support. Well tested.
- Content pipeline: bundled `wilderness-basics` pack (6 draft cards), manifest +
  YAML front-matter parser, workflow tags, provenance model, LIKE-based search.
- Maps: Mapsforge renders bundled Monaco fixture + user-imported `.map` files;
  GPS, distance/bearing, waypoint save/list/detail, breadcrumb recording, waypoint
  marker overlays, breadcrumb trail polylines, current-location dot/accuracy/heading,
  long-press waypoint creation, and waypoint-pin tap inspection are implemented.
- Guide: Ask (retrieval) + Workflows + Cards tabs; deterministic, cited, with an
  "I do not know from installed sources" fallback.
- Tests: 14 unit-test classes / 51 methods; data layer, parser, retrieval, workflows, and
  ViewModel state transitions are covered.

> Note: Guide "Ask"/Workflows (phases 7–8) are present in the current tree. The
> remaining Guide/Retrieval work is quality and depth work, not initial merge work.

**Known gaps / debt (the raw material for the departments):**
- ~~`PosaApp.kt` is a 773-line monolith~~ **RESOLVED** by the UI Architecture
  department (see "Solid / done"). Remaining seam: the two cross-domain reload
  callbacks are a clean interim boundary but still couple tools to map/guide
  writes; genuinely cutting them (tools load reacting to the waypoint/guide-card
  repositories directly) is a Local Data / Retrieval concern, not UI work.
- Maps: overlay core is implemented, but still needs manual device/emulator QA with
  the bundled Monaco fixture and at least one imported user map area.
- Guide content is thin (6 cards). Guide-card workflow membership is now
  data-driven via front-matter `workflow_tags`, but checklist/gear workflow matching
  still uses hardcoded terms.
- Retrieval still uses in-memory substring scoring. First-pass synonym expansion has
  landed, but there is no stemming, phrase/proximity weighting, externalized term
  config, or FTS-backed search yet.
- Minimal CI has landed. Remaining QA/release debt: no Compose UI tests, no
  coverage reporting.

---

## Departments

Each section is a **session seed**: open a new session in this repo, paste the
Scope + Tasks, and have the agent plan-then-build within that scope only.

### 1. UI Architecture  ✅ LANDED (branch `ui-architecture`)
**Goal:** replace the monolith with a testable ViewModel-backed architecture,
incrementally, without breaking the running app. Owner is hands-on here.

**Key files:** `ui/PosaApp.kt`, `ui/MapContent.kt`, `ui/ToolsContent.kt`,
`ui/GuideContent.kt`, the three `*Screens.kt`, `ui/PosaDestination.kt`, `MainActivity.kt`,
plus new `ui/MapViewModel.kt`, `ui/ToolsViewModel.kt`, `ui/GuideViewModel.kt`.

**Tasks (incremental, app builds after each step):**
1. ✅ Add a ViewModel layer (`androidx.lifecycle.viewmodel.compose`). **MapViewModel**
   owns `loadMapContent`, mutations, selection, and reload — `StateFlow<MapContentState>`.
2. ✅ **ToolsViewModel** and **GuideViewModel** done (guided question query,
   selected card/workflow, search, bundled-pack install).
3. ✅ Prop-drilling collapsed: `DestinationScreen` 19 params → 5; per-tab binding
   composables build the action holders from VM method refs.
4. ✅ `PosaApp.kt` reduced to navigation + scaffold + VM wiring only (338 lines).
5. ⏳ Cross-domain reload now isolated to two documented callbacks (not tokens);
   fully cutting them is deferred to Local Data / Retrieval (repository-reactive load).
6. ✅ Unit tests for each ViewModel (state transitions, mutation effects) — 14 new tests.

**Dependencies:** none upstream. Everything else builds on this, so it goes first.
**Verify:** ✅ app builds (`:app:assembleDebug`), all unit tests pass (51 total,
incl. new ViewModel tests).

### 2. Maps
**Goal:** draw field data on the map, not just list it. Owner is hands-on.

**Key files:** `ui/MapScreens.kt` (esp. `addMapLayer`, `WaypointPanel`,
`BreadcrumbPanel`), `ui/MapContent.kt`, `data/.../InstalledMapImporter.kt`, `ui/MapMath.kt`.

**Tasks:**
1. ✅ Render **waypoint markers** as a Mapsforge overlay; persist across restart;
   remove on delete; tapping a waypoint row centers/zooms to it. (Existing
   `docs/initial-issues.md` backlog item.)
2. ✅ Render **breadcrumb trails** as polylines on the map.
3. ✅ Render a **current-location** marker/dot that follows GPS updates, with
   accuracy ring and heading cone when available.
4. ✅ Long-press the map to create a waypoint; tapping waypoint pins opens details
   with edit/delete actions.
5. ⏳ Manual QA: bundled Monaco map, imported `.map`, save/delete waypoint, row
   centering, pin tap, long-press creation, breadcrumb recording, current-location
   marker/recenter behavior.

**Dependencies:** cleaner if UI Architecture lands first (overlay state in MapViewModel),
but the Mapsforge layer code is self-contained and can start in parallel if needed.
**Verify:** unit tests pass (`:app:testDebugUnitTest`). Still run app with bundled
Monaco map and an imported map; save waypoints → markers appear/persist/delete;
record a breadcrumb → polyline shows; confirm current-location dot/recenter behavior
on a device or emulator with a location fix.

### 3. Guide / Workflows
**Goal:** make guidance more organized and genuinely useful; de-hardcode matching.

**Key files:** `assets/packs/wilderness-basics/**`, `data/pack/PackManifestParser.kt`,
`data/pack/BundledPackInstaller.kt`, `ui/GuidedWorkflows.kt`, `ui/GuideContent.kt`,
`ui/GuideScreens.kt`.

**Tasks:**
1. **Brainstorm with owner** on content scope: decision trees (stay-put vs move when
   lost), regional/seasonal variants, per-domain depth (water treatment comparison,
   fire ignition reliability, shelter site selection, signal priority).
2. Expand/restructure guide cards; consider card-to-card links ("next"/prereq) and
   sub-categories.
3. ✅ Replace hardcoded `GuidedWorkflowDefinition` term lists with a data-driven mapping
   (e.g. workflow tags in card front-matter) so content changes don't silently break
   workflows. Guide cards now store parsed `workflow_tags`; legacy untagged cards
   still fall back to term matching.
4. ✅ Add `PackManifestParser` tests and workflow content tests.

**Dependencies:** feeds Retrieval. Light coupling to Local Data if front-matter schema changes.
**Verify:** pack installs; each workflow composes expected cards; parser tests pass.

### 4. Retrieval / RAG
**Goal:** improve ranking quality and add heavy, realistic test coverage.

**Key files:** `ui/GuidedQuestionRetrieval.kt`, `data/.../GuideCardDao` search, `ui/GuideContent.kt`.

**Tasks:**
1. Move retrieval to a data-layer/service class (Compose-independent); consider
   **SQLite FTS** instead of in-memory substring scoring.
2. ⏳ Improve ranking: stemming/normalization, synonym map (signal↔signaling),
   phrase/proximity weighting, replace arbitrary constants with tuned + documented
   values; keep the "unknown from installed sources" guarantee. First-pass synonym
   expansion is implemented and covered for water-treatment and signaling queries.
3. Externalize STOP_WORDS / MAP_CONTEXT_TERMS (config/resource, not source).
4. **Heavy test suite:** realistic query corpus, typo/edge cases, ranking regression
   tests, guarantee no unsupported-claim leakage.

**Dependencies:** benefits from Guide content depth (#3) and Local Data FTS (#5).
**Verify:** expanded retrieval test suite green; manual query spot-checks in app.

### 5. Local Data
**Goal:** harden the data layer for growth.

**Key files:** `data/local/PosaDatabase.kt`, `data/local/entity/Entities.kt`,
`data/local/dao/Daos.kt`, `data/repository/Repositories.kt`, seed/installer files.

**Tasks:**
1. Add SQLite **FTS** table for guide cards (supports Retrieval #1).
2. Migration discipline: document strategy, add tests for each new migration.
3. Constraints/indices: unique `(checklist_id, position)`, provenance indices,
   breadcrumb `ended >= started` sanity, batch upsert/delete helpers.
4. Keep Room types out of the UI (repository boundary hygiene).

**Dependencies:** mostly independent; FTS work coordinates with Retrieval #4.
**Verify:** migration tests pass; repository CRUD + new constraint tests pass.

### 6. QA / Release
**Goal:** automated safety net + repeatable release path. Stand up CI early even
though it's listed last — it protects every other workstream.

**Key files (new):** `.github/workflows/*.yml`, `app/build.gradle.kts` (test deps,
coverage), `docs/release-notes.md`, `README.md` status.

**Tasks:**
1. **GitHub Actions CI:** build + `testDebugUnitTest` on PR/push. CI should use the
   standard Android SDK setup action. Repo has **no `local.properties`** — local runs
   need `JAVA_HOME` → Android Studio JBR and `ANDROID_HOME`; document this.
2. Add **Compose UI test** harness (`androidx.compose.ui:ui-test-junit4`) + first screen tests.
3. Coverage reporting (JaCoCo or similar) with a baseline.
4. Release process: versioning, signed-build docs, release-notes discipline.

**Dependencies:** CI is wanted *before* heavy feature churn; UI-test harness is most
valuable after UI Architecture (#1) exposes testable seams.

---

## Sequencing & Dependencies

```
   #6 QA/Release — stand up CI FIRST (small), protects everything below
                         │
   #1 UI Architecture  ◀── START HERE (foundation)
        │
        ├──▶ #2 Maps (overlays live in MapViewModel)
        ├──▶ #3 Guide/Workflows ──▶ #4 Retrieval/RAG
        └──▶ #5 Local Data (FTS) ──▶ #4 Retrieval/RAG
```

**Recommended order:** (a) minimal CI from #6 → (b) #1 UI Architecture → then
parallelize #2 Maps and the #3→#4 / #5→#4 content+retrieval track → finish #6
(UI tests, coverage, release). Owner is hands-on in #1 and #2.

---

## Local build/test reference

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest      # unit tests
./gradlew :app:assembleDebug     # build
```
