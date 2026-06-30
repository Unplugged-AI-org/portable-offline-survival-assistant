# Map Data Strategy

POSA will use a Mapsforge-first strategy for offline OpenStreetMap-compatible maps.

## Decision

Use Mapsforge as the first map stack because POSA needs offline map files that users can load for selected areas without bundling large map data into the base app.

This does not mean POSA will try to compete with Organic Maps or OsmAnd in v0. The first goal is a focused offline map foundation.

## v0 Map Scope

Phase 5 should implement:

- Mapsforge dependency spike.
- Render one local test `.map` file.
- Current GPS coordinates.
- Save waypoint.
- Waypoint list and details.
- Distance and bearing.
- Breadcrumb recording.
- OSM attribution.

Phase 6 should implement:

- Android file picker import for supported map files.
- Installed map metadata.
- Enable/disable map areas.
- Clear errors for unsupported or missing files.

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
