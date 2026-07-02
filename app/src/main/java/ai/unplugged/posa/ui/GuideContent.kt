package ai.unplugged.posa.ui

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.repository.repositories
import ai.unplugged.posa.data.model.GuideCard
import ai.unplugged.posa.data.model.Pack
import ai.unplugged.posa.data.model.Provenance

internal data class GuideContentState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val packs: List<Pack> = emptyList(),
    val packCardCounts: Map<String, Int> = emptyMap(),
    val allCards: List<GuideCardItem> = emptyList(),
    val cards: List<GuideCardItem> = emptyList(),
)

internal data class GuideCardItem(
    val card: GuideCard,
    val pack: Pack?,
    val provenance: Provenance?,
)

internal suspend fun loadGuideContent(
    database: PosaDatabase,
    query: String,
): GuideContentState {
    val repositories = database.repositories()
    val packs = repositories.packs.list()
    val packById = packs.associateBy { it.id }
    val provenanceById = repositories.provenance.list().associateBy { it.id }
    val allCards = repositories.guideCards.list()
    val cards = if (query.isBlank()) {
        allCards
    } else {
        repositories.guideCards.search(query)
    }

    val allCardItems = allCards
        .map { card ->
            GuideCardItem(
                card = card,
                pack = packById[card.packId],
                provenance = card.provenanceId?.let { provenanceById[it] },
            )
        }
        .sortedWith(
            compareBy<GuideCardItem>(
                { it.card.category.lowercase() },
                { it.card.sortOrder },
                { it.card.title.lowercase() },
            ),
        )

    return GuideContentState(
        packs = packs,
        packCardCounts = allCards.groupingBy { it.packId }.eachCount(),
        allCards = allCardItems,
        cards = cards
            .map { card ->
                GuideCardItem(
                    card = card,
                    pack = packById[card.packId],
                    provenance = card.provenanceId?.let { provenanceById[it] },
                )
            }
            .sortedWith(
                compareBy<GuideCardItem>(
                    { it.card.category.lowercase() },
                    { it.card.sortOrder },
                    { it.card.title.lowercase() },
                ),
            ),
    )
}
