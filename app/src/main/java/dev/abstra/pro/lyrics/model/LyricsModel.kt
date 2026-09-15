package dev.abstra.pro.lyrics.model

data class LyricWord(
    val timeMs: Long,
    val text: String
)

data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val words: List<LyricWord> = emptyList()
) {
    val startTimeMs: Long get() = timeMs
    val content: String get() = text
}
