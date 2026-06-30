# Codex Session Plan

Use one Codex chat per phase. Each chat should start by reading this file, `ROADMAP.md`, and the specific docs relevant to that phase.

## Session 0: Repo Foundation

Goal: prepare the public repository.

Prompt:

```text
Read README.md, ROADMAP.md, docs/architecture.md, docs/safety-policy.md, docs/content-guidelines.md, docs/map-data-strategy.md, and docs/pack-schema.md.

Complete Phase 0 only. Do not create Android app code yet unless asked. Improve docs, issue templates, and public roadmap so the repository is ready to publish as UnpluggedAI/portable-offline-survival-assistant.
```

## Session 1: Android Skeleton

Goal: create the Kotlin/Compose app shell.

Prompt:

```text
Implement Phase 1 from ROADMAP.md.

Create a native Android Kotlin app named POSA with package ai.unplugged.posa. Use Jetpack Compose. Add four tabs: Map, Tools, Guide, Packs. Do not add cloud, accounts, analytics, AI, map downloads, or generated content. Keep the app buildable and document how to run it.
```

## Session 2: Local Data Foundation

Goal: add Room/SQLite core models.

Prompt:

```text
Implement Phase 2 from ROADMAP.md.

Add Room/SQLite local storage for waypoints, breadcrumbs, notes, checklists, gear/tools, packs, guide cards, and provenance metadata. Add repository interfaces and focused tests. Keep all data local.
```

## Session 3: Packs And Guide Cards

Goal: load local content packs.

Prompt:

```text
Implement Phase 3 from ROADMAP.md.

Add the bundled starter pack, manifest parser, guide-card loader, provenance UI, and local keyword or SQLite FTS search. Do not add AI generation. Do not include medical treatment guidance.
```

## Session 4: Checklists, Gear, And Notes

Goal: add practical user-owned field data.

Prompt:

```text
Implement Phase 4 from ROADMAP.md.

Add starter checklists, editable user checklists, gear/tool inventory with have/missing state, and local field notes linked to optional location, waypoint, checklist, guide card, or gear item.
```

## Session 5: Mapsforge Foundation

Goal: prove the offline map stack.

Prompt:

```text
Implement Phase 5 from ROADMAP.md.

Integrate Mapsforge enough to render a local test map file. Add location permission flow, coordinate display, save waypoint, waypoint list/detail, distance/bearing, and breadcrumb recording. Include OSM attribution. Do not implement routing or bulk map downloads.
```

## Session 6: User-Loaded Map Areas

Goal: let users load offline map files.

Prompt:

```text
Implement Phase 6 from ROADMAP.md.

Add Android file-picker import for supported Mapsforge map files, installed map metadata, enable/disable controls, and clear validation errors. Do not bundle large map data.
```

## Session 7: Guided Workflows

Goal: provide useful non-AI guidance.

Prompt:

```text
Implement Phase 7 from ROADMAP.md.

Add guided workflows for water, lost, shelter, fire, signaling, and battery conservation. Compose outputs from local guide cards, checklist steps, gear inventory, and location context. Show sources and missing-data warnings.
```

## Session 8: Retrieval And RAG v1

Goal: add source-grounded retrieval before optional model generation.

Prompt:

```text
Implement Phase 8 from ROADMAP.md.

Add a guided question interface over installed packs. Return cited source cards and excerpts. Include "I do not know from installed sources" behavior. Use gear and map context where appropriate. Do not add unsupported generated medical or survival claims.
```

## Session Rules

- Keep each session scoped to one phase.
- Run relevant tests before finishing.
- Update docs when behavior changes.
- Do not add delayed features unless the roadmap is explicitly updated first.
