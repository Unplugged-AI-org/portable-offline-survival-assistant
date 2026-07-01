package ai.unplugged.posa.data.local

import ai.unplugged.posa.data.model.InstalledMap
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.mapsforge.map.reader.MapFile
import java.io.File
import java.io.FileInputStream

object InstalledMapImporter {
    private const val MAP_EXTENSION = ".map"

    fun importFromUri(
        context: Context,
        uri: Uri,
        id: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): InstalledMap {
        val displayName = context.displayName(uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Imported map"
        val sanitizedName = displayName.sanitizeMapFileName()

        require(sanitizedName.endsWith(MAP_EXTENSION, ignoreCase = true)) {
            "Unsupported map file. Choose a Mapsforge .map file."
        }

        val mapDirectory = File(context.filesDir, "installed-maps").apply {
            mkdirs()
        }
        val destination = File(mapDirectory, "$id$MAP_EXTENSION")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Selected map file could not be opened.")

            require(destination.length() > 0L) {
                "Selected map file is empty."
            }
            validateMapsforgeFile(destination)
        } catch (exception: Exception) {
            destination.delete()
            throw exception
        }

        return InstalledMap(
            id = id,
            displayName = displayName.withoutMapExtension().ifBlank { sanitizedName },
            fileName = sanitizedName,
            filePath = destination.absolutePath,
            byteSize = destination.length(),
            isEnabled = true,
            importedAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    private fun validateMapsforgeFile(file: File) {
        try {
            val mapFile = MapFile(FileInputStream(file))
            mapFile.close()
        } catch (exception: Exception) {
            throw IllegalArgumentException("Selected file is not a readable Mapsforge map.", exception)
        }
    }

    private fun Context.displayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    return cursor.getString(displayNameIndex)
                }
            }
        }
        return null
    }

    private fun String.sanitizeMapFileName(): String =
        trim()
            .ifBlank { "imported.map" }
            .map { character ->
                when {
                    character.isLetterOrDigit() -> character
                    character == '.' || character == '-' || character == '_' -> character
                    else -> '_'
                }
            }
            .joinToString(separator = "")

    private fun String.withoutMapExtension(): String =
        if (endsWith(MAP_EXTENSION, ignoreCase = true)) {
            dropLast(MAP_EXTENSION.length)
        } else {
            this
        }
}
