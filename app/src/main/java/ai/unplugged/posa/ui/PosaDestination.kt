package ai.unplugged.posa.ui

enum class PosaDestination(
    val label: String,
    val headline: String,
    val summary: String,
    val offlineState: String,
    val nextSteps: List<String>,
) {
    Map(
        label = "Map",
        headline = "Offline map workspace",
        summary = "Render a local Mapsforge test map, save waypoints, inspect distance and bearing, and record breadcrumb trails.",
        offlineState = "The bundled test map renders from local assets. Location stays on-device, and no map downloads, routing, sync, accounts, telemetry, or AI generation is active.",
        nextSteps = listOf(
            "Use Phase 6 to import user-selected Mapsforge map files.",
            "Keep OpenStreetMap attribution visible wherever map data is shown.",
            "Treat map data as field context, not survival-critical truth.",
        ),
    ),
    Tools(
        label = "Tools",
        headline = "Field tools",
        summary = "Create and edit local checklists, gear inventory, and timestamped field notes.",
        offlineState = "Tools data is stored only in the local Room database; no sync, accounts, telemetry, or AI generation is active.",
        nextSteps = listOf(
            "Use checklists for planning and gear status before later guided workflows read this context.",
            "Link notes to saved map waypoints when location context matters.",
            "Keep notes, checklist edits, and inventory changes local by default.",
        ),
    ),
    Guide(
        label = "Guide",
        headline = "Source-aware guide",
        summary = "Guide cards load from installed offline content packs and keep source details visible.",
        offlineState = "Bundled guide cards are installed from local app assets; no generated survival or medical advice is included.",
        nextSteps = listOf(
            "Replace draft cards with reviewed release-ready content.",
            "Expand pack validation before user imports are supported.",
            "Return only installed-source-backed guidance.",
        ),
    ),
    Packs(
        label = "Packs",
        headline = "Offline content packs",
        summary = "Installed guide packs are managed locally with license and review metadata.",
        offlineState = "Pack records are stored locally; no registry, downloads, accounts, or network calls are wired in.",
        nextSteps = listOf(
            "Add signature and schema validation before external pack import.",
            "Keep official, community, and user-imported packs visually distinct.",
            "Support user-loaded map areas in a later phase.",
        ),
    ),
}
