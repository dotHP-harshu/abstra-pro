package dev.abstra.pro.lyrics.util

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object CharsetUtils {

    private val GB18030_CHARSET by lazy {
        try {
            Charset.forName("GB18030")
        } catch (_: Exception) {
            try {
                Charset.forName("GBK")
            } catch (_: Exception) {
                Charsets.UTF_8
            }
        }
    }

    private val SHIFT_JIS_CHARSET by lazy {
        try {
            Charset.forName("Shift_JIS")
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    private val BIG5_CHARSET by lazy {
        try {
            Charset.forName("Big5")
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    /**
     * Decodes a byte array automatically detecting if it's UTF-8 (with or without BOM),
     * GB18030 / GBK (Chinese), Shift-JIS (Japanese), Big5 (Traditional Chinese) or ISO-8859-1.
     */
    fun decodeBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // 1. Detect BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        // 2. Attempt strict UTF-8
        try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Not valid UTF-8, try common Asian encodings
        }

        // 3. Try GB18030 (covers GBK and GB2312)
        try {
            val decoder = GB18030_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continue
        }

        // 4. Try Shift-JIS
        try {
            val decoder = SHIFT_JIS_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continue
        }

        // 5. Try Big5
        try {
            val decoder = BIG5_CHARSET.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            return sanitizeText(decoded)
        } catch (_: Exception) {
            // Continue
        }

        // 6. Fallback to permissive UTF-8 or ISO-8859-1
        val fallback = String(bytes, Charsets.UTF_8)
        return sanitizeText(fallback)
    }

    /**
     * Reads a text file, automatically detecting the character encoding.
     */
    fun readText(file: File): String {
        return try {
            val bytes = file.readBytes()
            decodeBytes(bytes)
        } catch (_: Exception) {
            file.readText()
        }
    }

    /**
     * Fixes common mojibake cases, such as:
     * - "R路I路O路T" -> "R·I·O·T" (when middle dot U+00B7 was mistakenly read as GBK)
     * - ISO-8859-1 mojibake that was originally UTF-8 or GBK
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return input ?: ""
        var text = input

        // Reported case: "R路I路O路T" -> "R·I·O·T"
        // In GBK, 0xC2 0xB7 (which is "·" in UTF-8) is decoded as "路" (lu).
        text = text.replace(Regex("(?<=[A-Za-z0-9])\\s*路\\s*(?=[A-Za-z0-9])"), "·")

        // Classic ISO-8859-1 -> UTF-8 mojibake repair
        if (looksLikeIsoUtf8Mojibake(text)) {
            try {
                val isoBytes = text.toByteArray(Charsets.ISO_8859_1)
                val candidateUtf8 = String(isoBytes, Charsets.UTF_8)
                if (candidateUtf8.length < text.length) {
                    text = candidateUtf8
                }
            } catch (_: Exception) {
                // Keep original
            }
        }

        return text
    }

    private fun looksLikeIsoUtf8Mojibake(text: String): Boolean {
        return text.contains("Ã") || text.contains("Â·") || text.contains("Â")
    }
}
