# POSA — Workstreams / Department Program Plan

This is the master program plan for POSA after roadmap phases 0–8. Instead of
working phase-by-phase, ongoing work is organized into parallel **departments**,
each run in its own focused session and its own branch/PR. This document is the
source of truth: update the "Current State Snapshot" as departments land work.

**Working model (agreed):**
- One session per department. This document seeds each session.
- Branch per department (e.g. `ui-architecture`, `maps-overlays`); one concern per PR.
- First department: **UI Architecture**. UI approach: **incremental ViewModel extraction**
  (the app must build and run after each step).

---

## Current State Snapshot

**Solid / done (roadmap phases 0–8):**
- Data layer: 11 Room entities, 9 DAOs, 9 repositories, mappers, 2 migrations
  (v1→v3), in-memory test support. Well tested.
- Content pipeline: bundled `wilderness-basics` pack (6 draft cards), manifest +
  YAML front-matter parser, provenance model, LIKE-based search.
- Maps: Mapsforge renders bundled Monaco fixture + user-imported `.map` files;
  GPS, distance/bearing, breadcrumb recording all persist.
- Guide: Ask (retrieval) + Workflows + Cards tabs; deterministic, cited, with an
  "I do not know from installed sources" fallback.
- Tests: 10 unit-test classes / 27 methods; data layer + retrieval + workflows
  well covered.

> Note: the Guide "Ask"/Workflows (phases 7–8) currently live on the unmerged
> `phase-8-retrieval-rag` branch. Merge that before or alongside starting the
> Guide/Retrieval departments.

**Known gaps / debt (the raw material for the departments):**
- `PosaApp.kt` is a 773-line monolith: state hoisting, DB mutations, file I/O,
  `LaunchedEffect` loaders, and layout dispatch all in one composable. No
  ViewModel layer. Heavy prop-drilling (15+ params into `DestinationScreen`).
  Cross-domain coupling via reload tokens.
- Maps: waypoints, breadcrumbs, and current location are **text lists only** —
  nothing is drawn on the map surface.
- Guide content is thin (6 cards) and workflow term-matching is hardcoded and
  fragile (string lists coupled to card titles/categories).
- Retrieval is naive substring scoring: no stemming/synonyms, English/US-locale
  baked in, hand-maintained STOP_WORDS / MAP_CONTEXT_TERMS, arbitrary constants.
- No CI (`.github/workflows` has only issue templates). No Compose UI tests. No
  `PackManifestParser` tests. No coverage reporting.

---

## Departments

Each section is a **session seed**: open a new session in this repo, paste the
Scope + Tasks, and have the agent plan-then-build within that scope only.

### 1. UI Architecture  ▶ FIRST
**Goal:** replace the monolith with a testable ViewModel-backed architecture,
incrementally, without breaking the running app. Owner is hands-on here.

**Key files:** `ui/PosaApp.kt`, `ui/MapContent.kt`, `ui/ToolsContent.kt`,
`ui/GuideContent.kt`, the three `*Screens.kt`, `ui/PosaDestination.kt`, `MainActivity.kt`.

**Tasks (incremental, app builds after each step):**
1. Add a ViewModel layer (`androidx.lifecycle.viewmodel.compose`). Establish the
   pattern with **MapViewModel** first: move `loadMapContent`, `MapActions`
   mutations, and reload-token logic out of `PosaApp.kt`; expose `StateFlow<MapContentState>`.
2. Repeat for **ToolsViewModel**, then **GuideViewModel** (guided question query,
   selected card/workflow, search).
3. Collapse prop-drilling: each screen takes its ViewModel (or a small state +
   callbacks holder), not 15 loose params.
4. Reduce `PosaApp.kt` to navigation + scaffold + VM wiring only.
5. Decouple cross-domain reload tokens (map mutation shouldn't poke tools).
6. Add unit tests for each ViewModel (state transitions, mutation effects).

**Dependencies:** none upstream. Everything else builds on this, so it goes first.
**Verify:** app builds/launches, all four tabs navigate, existing unit tests pass,
new ViewModel tests pass.

### 2. Maps
**Goal:** draw field data on the map, not just list it. Owner is hands-on.

**Key files:** `ui/MapScreens.kt` (esp. `addMapLayer`, `WaypointPanel`,
`BreadcrumbPanel`), `ui/MapContent.kt`, `data/.../InstalledMapImporter.kt`, `ui/MapMath.kt`.

**Tasks:**
1. Render **waypoint markers** as a Mapsforge overlay; persist across restart;
   remove on delete; tapping a waypoint row centers/zooms to it. (Existing
   `docs/initial-issues.md` backlog item.)
2. Render **breadcrumb trails** as polylines on the map.
3. Render a **current-location** marker/dot that follows GPS updates.
4. Optional: tap-on-map to create a waypoint; long-press context.

**Dependencies:** cleaner if UI Architecture lands first (overlay state in MapViewModel),
but the Mapsforge layer code is self-contained and can start in parallel if needed.
**Verify:** run app with bundled Monaco map; save waypoints → markers appear/persist/delete;
record a breadcrumb → polyline shows.

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
3. Replace hardcoded `GuidedWorkflowDefinition` term lists with a data-driven mapping
   (e.g. workflow tags in card front-matter) so content changes don't silently break
   workflows.
4. Add `PackManifestParser` tests (currently none) and workflow content tests.

**Dependencies:** feeds Retrieval. Light coupling to Local Data if front-matter schema changes.
**Verify:** pack installs; each workflow composes expected cards; parser tests pass.

### 4. Retrieval / RAG
**Goal:** improve ranking quality and add heavy, realistic test coverage.

**Key files:** `ui/GuidedQuestionRetrieval.kt`, `data/.../GuideCardDao` search, `ui/GuideContent.kt`.

**Tasks:**
1. Move retrieval to a data-layer/service class (Compose-independent); consider
   **SQLite FTS** instead of in-memory substring scoring.
2. Improve ranking: stemming/normalization, synonym map (signal↔signaling),
   phrase/proximity weighting, replace arbitrary constants with tuned + documented
   values; keep the "unknown from installed sources" guarantee.
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
