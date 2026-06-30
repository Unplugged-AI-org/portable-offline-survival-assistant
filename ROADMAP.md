# POSA Roadmap

This roadmap is designed for multiple Codex sessions. Each phase should be small enough to build, review, and test independently.

## Phase 0: Public Repo Foundation

Goal: create a public-ready planning and governance foundation before app code.

Deliverables:

- README, roadmap, license, contribution guide, and code of conduct.
- Architecture, safety, content, pack schema, map strategy, and build-in-public docs.
- GitHub issue templates and initial issue list.
- Clear v0 scope, non-goals, and safety boundaries.

Done when:

- A new contributor can understand what POSA is, what it is not, and what to build first.
- The repo is safe to make public.

## Phase 1: Android Skeleton

Goal: create the Android app shell without implementing the hard subsystems yet.

Deliverables:

- Kotlin Android app using Jetpack Compose.
- Default package name: `ai.unplugged.posa`.
- Four tabs: Map, Tools, Guide, Packs.
- Basic POSA branding and offline-first copy.
- No cloud, account, AI, or analytics dependency.

Done when:

- The app builds, launches, and navigates between the four tabs.

## Phase 2: Local Data Foundation

Goal: establish the local-first data layer.

Deliverables:

- Room/SQLite database.
- Models for waypoints, breadcrumbs, notes, checklists, gear/tools, packs, guide cards, and provenance.
- Repository interfaces around local storage.
- Seed/sample data path for development.

Done when:

- Core objects can be created, read, updated, and deleted locally.
- Airplane-mode behavior does not break core app flows.

## Phase 3: Packs and Guide Cards

Goal: make POSA useful as a cited offline guide before adding AI.

Deliverables:

- Bundled starter pack.
- Pack manifest parser.
- Markdown or structured guide-card loader.
- Guide card list/detail views.
- Source/provenance display on every card.
- Local keyword search, preferably SQLite FTS.

Done when:

- A bundled pack loads locally.
- Users can search and read guide cards with source metadata visible.

## Phase 4: Checklists, Gear, and Field Notes

Goal: add practical user-owned field data.

Deliverables:

- Starter survival checklists.
- User-created checklists and checklist items.
- Gear/tool inventory with "have" and "missing" state.
- Field notes with title, body, timestamp, and optional links to location, waypoint, checklist, guide card, or gear item.

Done when:

- A user can plan, record field observations, and store gear context locally.

## Phase 5: Mapsforge Map Foundation

Goal: start with a real offline OSM-compatible map stack while keeping scope controlled.

Deliverables:

- Mapsforge dependency spike and integration.
- Render a local Mapsforge-compatible test map file.
- Current GPS coordinate display.
- Save waypoint from current location.
- Waypoint list and details.
- Distance and bearing to selected waypoint.
- Breadcrumb start/stop recording.
- Required OSM attribution visible in map-related UI/docs.

Done when:

- The app can show a local offline map file and basic location/waypoint features without internet.

## Phase 6: User-Loaded Map Areas

Goal: let users add offline map areas without bundling large map data in the base app.

Deliverables:

- Import local map file from Android storage picker.
- Store map metadata locally.
- Enable/disable installed map areas.
- Basic validation and useful error states.
- Documentation for where supported test map files can come from.

Done when:

- A user can load a supported offline map area and use it without network access.

## Phase 7: Guided Workflows

Goal: provide structured, non-AI survival flows that combine local context.

Deliverables:

- Initial flows: "I need water", "I am lost", "I need shelter", "I need fire", "I need to signal", and "save battery".
- Flow output composed from guide cards, checklist steps, gear inventory, and location context where available.
- Clear source links and warnings when data is missing.

Done when:

- The app can produce useful source-backed guidance without generation.

## Phase 8: Retrieval and RAG v1

Goal: add source-grounded retrieval before optional model generation.

Deliverables:

- Query interface over installed packs.
- Retrieved source cards and excerpts.
- "I do not know from installed sources" behavior.
- Confidence/provenance UI.
- Gear and map context included in retrieval context where appropriate.
- No generated medical treatment advice.

Done when:

- Users can ask guided questions and get cited local results without unsupported claims.

## Later Phases

Later work may include optional local model support, semantic search, GPX import/export, map POI/resource packs, QR export/import, pack registry, verified community packs, Meshtastic, Wi-Fi Direct, LAN sync, and signed field notes.
