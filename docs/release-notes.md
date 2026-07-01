# Release Notes

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

- Room schema export is temporarily disabled due to Room 2.8.4 / KSP / serialization clean-build issue.
