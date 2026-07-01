# Map Data Strategy

POSA will use a Mapsforge-first strategy for offline OpenStreetMap-compatible maps.

## Decision

Use Mapsforge as the first map stack because POSA needs offline map files that users can load for selected areas without bundling large map data into the base app.

This does not mean POSA will try to compete with Organic Maps or OsmAnd in v0. The first goal is a focused offline map foundation.

## v0 Map Scope

Phase 5 should implement:

- Mapsforge dependency spike. Implemented with Maven Central `org.mapsforge` Android artifacts.
- Render one local test `.map` file. Implemented with a tiny bundled `maps/monaco.map` asset.
- Current GPS coordinates. Implemented behind Android fine/coarse location permission.
- Save waypoint. Implemented from current device location.
- Waypoint list and details. Implemented in the Map tab.
- Distance and bearing. Implemented from the current fix to selected/saved waypoints.
- Breadcrumb recording. Implemented with start/stop and locally stored points.
- OSM attribution. Implemented in map UI and repository docs.

Phase 6 implements:

- Android file picker import for supported Mapsforge `.map` files.
- Local copy into app-private storage so imported areas remain available offline.
- Installed map metadata in Room, including Mapsforge start position, optional start zoom, and bounding box-derived viewport data.
- Enable/disable controls for installed map areas. Importing or enabling a map makes it the active offline area and disables other installed maps.
- Automatic viewport focus for the active installed map on import, enable, and app restart. The bundled Monaco fixture is used only when no installed map is enabled.
- Clear errors for unsupported, unreadable, empty, or missing files.

## Out Of Scope For v0

- Full offline routing.
- Turn-by-turn navigation.
- Topographic/contour layers.
- Bulk map downloads from OpenStreetMap public tile servers.
- Large bundled map datasets.
- Map pack registry.
- Map data generation pipeline.

## Licensing And Attribution

OpenStreetMap-derived data has licensing and attribution obligations. POSA must preserve attribution in app UI and docs wherever OSM-derived map data is used.

Do not build a feature that bulk-downloads public OSM tiles. POSA should use user-loaded offline map files, bundled tiny test fixtures if licensing allows, or a clearly documented future map-pack pipeline.

Phase 5 bundles `app/src/main/assets/maps/monaco.map` as a small local Mapsforge rendering fixture:

- Source: https://download.mapsforge.org/maps/v5/europe/monaco.map
- Source index size: 342K on 2026-07-01
- Purpose: local renderer verification only, not a production map area.

For Phase 6 development and manual testing, use supported Mapsforge `.map` files from the Mapsforge public download index:

- https://download.mapsforge.org/maps/v5/

Prefer the smallest suitable region file when testing imports. User-loaded maps should be treated as user-provided offline data; POSA should not bulk-download OpenStreetMap public tiles or silently add large map datasets to the base app.

Relevant references:

- https://www.openstreetmap.org/copyright
- https://github.com/mapsforge/mapsforge

## Risks

- Map files can be large.
- Android storage permissions and file access differ by OS version.
- Offline map rendering can affect battery and memory.
- User-loaded map files may be outdated or incomplete.
- OSM data should not be treated as survival-critical truth.

The app should show uncertainty where map-derived field guidance is used.
