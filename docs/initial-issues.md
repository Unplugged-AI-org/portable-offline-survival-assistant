# Initial GitHub Issues

These issues can be created after the GitHub repo exists at `Unplugged-AI-org/portable-offline-survival-assistant`.

## Phase 0

- Prepare public README and roadmap.
- Add safety policy.
- Add content guidelines.
- Add pack schema draft.
- Add map data strategy.
- Add contribution guide and code of conduct.

## Phase 1

- Initialize Android Kotlin project.
- Add Jetpack Compose navigation.
- Add Map tab placeholder.
- Add Tools tab placeholder.
- Add Guide tab placeholder.
- Add Packs tab placeholder.
- Add offline-first status copy.

## Phase 2

- Add Room database.
- Add waypoint model.
- Add breadcrumb model.
- Add field note model.
- Add checklist model.
- Add gear/tool inventory model.
- Add pack and guide card models.
- Add provenance metadata model.

## Phase 3

- Add starter pack fixture.
- Parse pack manifest.
- Load guide cards.
- Display guide card provenance.
- Add local guide search.

## Phase 4

- Add starter checklists.
- Add editable checklists.
- Add gear inventory.
- Add field notes.
- Link notes to waypoints, checklists, guide cards, and gear.

## Phase 5

- Research Mapsforge Android setup.
- Render local Mapsforge test map.
- Add location permission flow.
- Add waypoint save/list/detail.
- Add distance and bearing.
- Add breadcrumb recording.
- Add OSM attribution.

## Phase 6

- Add map file import.
- Store installed map metadata.
- Add map enable/disable controls.
- Add unsupported file errors.

## Phase 7

- Add "I need water" guided workflow.
- Add "I am lost" guided workflow.
- Add "I need shelter" guided workflow.
- Add "I need fire" guided workflow.
- Add "I need to signal" guided workflow.
- Add "save battery" guided workflow.

## Phase 8

- Add guided question interface.
- Return cited source cards.
- Add source excerpt display.
- Add "I do not know from installed sources" behavior.
- Add retrieval context from gear and map state.

## Backlog

### Render saved waypoints on Mapsforge map

Status: Backlog / Map UI polish

Problem:
Saved waypoints are persisted and visible in guided workflow location context, but they do not render visually on the Map screen.

Current behavior:

- User can save waypoint.
- Workflow can read and display saved waypoint.
- Map view does not show marker overlay.

Expected behavior:

- Saved waypoints render as visible markers on the offline map.
- Markers persist after app restart.
- Deleted waypoints disappear from the map.
- Selecting a waypoint centers the map on that waypoint if practical.

Scope:

- Map UI / overlay rendering only.
- No routing.
- No sync.
- No AI.
- No new map downloads.
- No Phase 8 retrieval work.
