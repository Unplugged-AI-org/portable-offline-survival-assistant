package ai.unplugged.posa.data.pack

import ai.unplugged.posa.data.local.PosaDatabase
import ai.unplugged.posa.data.local.toEntity
import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PackInstallResult(
    val packId: String,
    val packTitle: String,
    val cardsInstalled: Int,
)

object BundledPackInstaller {
    private val starterPackAssetPaths = listOf("packs/wilderness-basics")

    suspend fun installAll(
        context: Context,
        database: PosaDatabase,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): List<PackInstallResult> = withContext(Dispatchers.IO) {
        starterPackAssetPaths.map { assetPath ->
            installPack(context.applicationContext, database, assetPath, nowEpochMillis)
        }
    }

    private suspend fun installPack(
        context: Context,
        database: PosaDatabase,
        assetPath: String,
        nowEpochMillis: Long,
    ): PackInstallResult {
        val manifest = PackManifestParser.parseManifest(
            context.readAssetText("$assetPath/manifest.json"),
        )
        val parsedCards = manifest.files.map { relativePath ->
            PackManifestParser.parseGuideCard(
                manifest = manifest,
                relativePath = relativePath,
                markdown = context.readAssetText("$assetPath/$relativePath"),
                nowEpochMillis = nowEpochMillis,
            )
        }

        database.withTransaction {
            val packDao = database.packDao()
            val guideCardDao = database.guideCardDao()
            val provenanceDao = database.provenanceDao()
            val existingPack = packDao.get(manifest.id)
            val pack = PackManifestParser.toPack(
                manifest = manifest,
                installedAtEpochMillis = existingPack?.installedAtEpochMillis ?: nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis,
            )
            val installedCardIds = parsedCards.map { it.card.id }.toSet()

            packDao.upsert(pack.toEntity())
            guideCardDao.listForPack(manifest.id)
                .filterNot { it.id in installedCardIds }
                .forEach { guideCardDao.delete(it.id) }

            parsedCards.forEach { parsedCard ->
                val existingCard = guideCardDao.get(parsedCard.card.id)
                val card = parsedCard.card.copy(
                    createdAtEpochMillis = existingCard?.createdAtEpochMillis ?: nowEpochMillis,
                    updatedAtEpochMillis = nowEpochMillis,
                )
                provenanceDao.upsert(parsedCard.provenance.toEntity())
                guideCardDao.upsert(card.toEntity())
            }
        }

        return PackInstallResult(
            packId = manifest.id,
            packTitle = manifest.title,
            cardsInstalled = parsedCards.size,
        )
    }

    private fun Context.readAssetText(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
