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
    Ask(
        label = "Ask",
        headline = "Source Q&A",
        summary = "Search the bundled source corpus with local keyword and embedding retrieval.",
        offlineState = "Questions search the local RAG database and bundled query embedding model on-device; results are source excerpts, citations, and retrieval metadata.",
        nextSteps = listOf(
            "Add answer generation only after verifier and repair checks are wired.",
            "Keep source citations visible for every retrieved excerpt.",
            "Use keyword fallback whenever local embeddings are unavailable.",
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
}
