# POSA

Portable Offline Survival Assistant, built by Unplugged AI.

POSA is a free, open-source, Android-first survival companion for hikers, campers, preppers, overlanders, CERT-style users, and other people who need practical offline field tools without relying on cloud services or cell signal.

The goal is not to build a generic survival chatbot. POSA should feel like a field instrument: fast, local-first, source-aware, and useful even when no AI model, account, backend, or internet connection is available.

## Status

This repository is at Phase 0: public planning and architecture.

No production Android app is implemented yet. The current work is defining the roadmap, safety boundaries, content model, map strategy, and session-by-session build plan.

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

## Safety Note

POSA is not a substitute for professional emergency, medical, legal, or rescue advice. The app must clearly show sources, review status, and uncertainty for field guidance. When emergency services are available, users should contact them.

## Attribution

OpenStreetMap data and any derived map packs must preserve required attribution and comply with applicable licenses, including ODbL where relevant.
