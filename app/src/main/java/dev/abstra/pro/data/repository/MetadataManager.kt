package dev.abstra.pro.data.repository

import android.content.Context
import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/**
 * Handles physical file audio tag inspection and writing using jaudiotagger.
 * Used when the user selects "Write to file tags" mode.
 */
object MetadataManager {
    private const val TAG = "MetadataManager"

    fun writeMetadataToFile(
        filePath: String,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?
    ): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canWrite()) {
                Log.w(TAG, "File cannot be written: $filePath")
                return false
            }

            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault

            title?.let { tag.setField(FieldKey.TITLE, it) }
            artist?.let { tag.setField(FieldKey.ARTIST, it) }
            album?.let { tag.setField(FieldKey.ALBUM, it) }
            genre?.let { tag.setField(FieldKey.GENRE, it) }

            audioFile.commit()
            Log.i(TAG, "Successfully wrote metadata tags to $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing metadata to file: $filePath", e)
            false
        }
    }

    /**
     * Reads embedded lyrics (USLT / LYRICS frames) from an audio file via
     * jaudiotagger. Returns null when the file has no usable lyrics tag.
     */
    fun readEmbeddedLyrics(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return null
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return null
            val lyrics = tag.getFirst(FieldKey.LYRICS)
            if (lyrics.isNullOrBlank()) null else lyrics
        } catch (e: Exception) {
            Log.d(TAG, "No embedded lyrics in $filePath: ${e.message}")
            null
        }
    }
}
