# Architecture

POSA is an Android-first, offline-first app. The initial implementation should be native Kotlin with Jetpack Compose, Room/SQLite, and Mapsforge for offline OSM-compatible maps.

## Core Layers

App UI:

- Map
- Tools
- Guide
- Packs

Local data:

- Waypoints
- Breadcrumbs
- Field notes
- Checklists
- Gear/tools inventory
- Installed packs
- Guide cards
- Source/provenance metadata

Content system:

- Pack manifest parser.
- Bundled starter pack.
- Local pack storage.
- Guide card loader.
- Search index.
- Provenance display.
- Guided workflow composer over source cards, checklist items, gear inventory, and saved map context.
- Source-grounded retrieval over installed packs with cited excerpts, confidence scoring, gear/map context, and "I do not know from installed sources" fallback.

Map system:

- Mapsforge renderer.
- User-loaded offline map files.
- Current location.
- Waypoints.
- Breadcrumbs.
- Distance and bearing.

Future retrieval:

- SQLite FTS over guide cards.
- Guided question interface.
- Retrieved source cards.
- Optional semantic retrieval later.
- Optional local or LAN model later.

## Hard Constraints

- No required network.
- No account system.
- No cloud backend.
- No required model.
- No hidden analytics.
- No large bundled map data.
- No unreviewed medical treatment guidance.

## Default Android Choices

- Language: Kotlin.
- UI: Jetpack Compose.
- Storage: Room on SQLite.
- Search: SQLite FTS where practical.
- Maps: Mapsforge first.
- Package: `ai.unplugged.posa`.

These defaults should be changed only with a documented reason.
