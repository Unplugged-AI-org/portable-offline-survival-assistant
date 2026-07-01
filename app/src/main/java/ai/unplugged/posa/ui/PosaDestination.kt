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
        summary = "Map rendering and GPS land in later phases; waypoint and breadcrumb storage is local-ready.",
        offlineState = "No map downloads, network calls, or location permissions are active in this shell.",
        nextSteps = listOf(
            "Wire waypoint and breadcrumb repositories into the future map UI.",
            "Reserve space for a local Mapsforge map view.",
            "Keep OpenStreetMap attribution visible when map data is added.",
        ),
    ),
    Tools(
        label = "Tools",
        headline = "Field tools",
        summary = "Checklists, gear, field notes, and practical calculators will live here.",
        offlineState = "Local storage models exist for user-owned tools data; no sync or telemetry is active.",
        nextSteps = listOf(
            "Add editable checklists, notes, and inventory in Phase 4.",
            "Connect future screens to Room-backed repositories.",
            "Avoid analytics, accounts, and cloud sync by default.",
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
