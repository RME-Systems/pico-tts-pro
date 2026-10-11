package com.rme.picottspro

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

class TextProcessor(
    private val context: Context,
    private val settings: SettingsManager
) {

    companion object {
        private const val TAG = "TextProcessor"
    }

    // Cache of loaded emoji maps per locale to avoid re-reading disk
    private val emojiCache = HashMap<String, Map<String, String>>()

    private val punctuationSpanishMap = mapOf(
        '.' to " punto ",
        ',' to " coma ",
        ';' to " punto y coma ",
        ':' to " dos puntos ",
        '!' to " exclamación ",
        '¡' to " abre exclamación ",
        '?' to " interrogación ",
        '¿' to " abre interrogación ",
        '(' to " abre paréntesis ",
        ')' to " cierra paréntesis ",
        '[' to " abre corchete ",
        ']' to " cierra corchete ",
        '{' to " abre llave ",
        '}' to " cierra llave ",
        '"' to " comillas ",
        '\'' to " apóstrofe ",
        '-' to " guion ",
        '/' to " barra ",
        '\\' to " barra inversa ",
        '@' to " arroba ",
        '#' to " almohadilla ",
        '%' to " por ciento ",
        '&' to " et ",
        '*' to " asterisco ",
        '+' to " más ",
        '=' to " igual ",
        '<' to " menor que ",
        '>' to " mayor que ",
        '_' to " guion bajo "
    )

    private val punctuationEnglishMap = mapOf(
        '.' to " dot ",
        ',' to " comma ",
        ';' to " semicolon ",
        ':' to " colon ",
        '!' to " exclamation ",
        '?' to " question mark ",
        '(' to " open paren ",
        ')' to " close paren ",
        '[' to " open bracket ",
        ']' to " close bracket ",
        '{' to " open brace ",
        '}' to " close brace ",
        '"' to " quote ",
        '\'' to " apostrophe ",
        '-' to " hyphen ",
        '/' to " slash ",
        '\\' to " backslash ",
        '@' to " at ",
        '#' to " hash ",
        '%' to " percent ",
        '&' to " and ",
        '*' to " asterisk ",
        '+' to " plus ",
        '=' to " equals ",
        '<' to " less than ",
        '>' to " greater than ",
        '_' to " underscore "
    )

    fun process(rawText: String, activeLocale: Locale): String {
        if (rawText.isEmpty()) return rawText
        var text = rawText

        // 1. Process Emojis: single-pass scanner, zero-delay
        if (settings.isReadEmojis && containsPotentialEmoji(text)) {
            text = processEmojisSinglePass(text, activeLocale)
        }

        // 2. Process Punctuation
        val punctLevel = settings.punctuationLevel
        if (punctLevel != "none") {
            text = processPunctuation(text, punctLevel, activeLocale)
        }

        return text
    }

    /**
     * Emojis are in higher Unicode code ranges (>= 0x2000 for symbols, >= 0x1F000 for standard emojis).
     * Spanish letters like á, é, í, ó, ú, ñ, ü are <= 0x0100 and will NOT trigger this check.
     */
    private fun containsPotentialEmoji(s: String): Boolean {
        var i = 0
        val len = s.length
        while (i < len) {
            val cp = Character.codePointAt(s, i)
            if (cp >= 0x2000) {
                return true
            }
            i += Character.charCount(cp)
        }
        return false
    }

    private fun getEmojiMap(langCode: String): Map<String, String> {
        val normalized = when (langCode) {
            "es", "spa" -> "es"
            "en", "eng" -> "en"
            "fr", "fra" -> "fr"
            "de", "deu" -> "de"
            "it", "ita" -> "it"
            else -> "en"
        }

        synchronized(emojiCache) {
            val cached = emojiCache[normalized]
            if (cached != null) return cached

            val map = HashMap<String, String>(4000)
            try {
                val assetPath = "emojis/emojis_$normalized.json"
                context.assets.open(assetPath).use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        val jsonString = reader.readText()
                        val json = JSONObject(jsonString)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = json.getString(key)
                        }
                    }
                }
                Log.i(TAG, "Loaded ${map.size} emojis for language '$normalized'")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load emoji table for '$normalized': ${e.message}")
            }
            emojiCache[normalized] = map
            return map
        }
    }

    /**
     * Single-pass scanner: reads through the string once O(N), looking up emoji sequences
     * in the HashMap O(1). No 4000-iteration loops.
     */
    private fun processEmojisSinglePass(text: String, activeLocale: Locale): String {
        val targetLang = when (val pref = settings.emojiLanguage) {
            "auto" -> activeLocale.language
            else -> pref
        }

        val dictionary = getEmojiMap(targetLang)
        if (dictionary.isEmpty()) return text

        val sb = StringBuilder(text.length + 32)
        var i = 0
        val len = text.length

        while (i < len) {
            val cp = Character.codePointAt(text, i)
            val charCount = Character.charCount(cp)

            if (cp < 0x2000) {
                // Regular character, copy directly
                sb.appendCodePoint(cp)
                i += charCount
                continue
            }

            // Potential emoji: look ahead up to 10 characters for composite emojis (ZWJ, skin tones)
            var matched = false
            val maxLookahead = minOf(len, i + 12)
            for (end in maxLookahead downTo i + charCount) {
                val candidate = text.substring(i, end)
                val description = dictionary[candidate]
                if (description != null) {
                    sb.append(" ").append(description).append(" ")
                    i = end
                    matched = true
                    break
                }
            }

            if (!matched) {
                // Standalone character not in emoji dictionary
                sb.appendCodePoint(cp)
                i += charCount
            }
        }

        return sb.toString()
    }

    private fun processPunctuation(text: String, level: String, activeLocale: Locale): String {
        val isSpanish = activeLocale.language.startsWith("es")
        val punctMap = if (isSpanish) punctuationSpanishMap else punctuationEnglishMap

        val sb = StringBuilder(text.length * 2)
        for (char in text) {
            when (level) {
                "all" -> {
                    val spoken = punctMap[char]
                    if (spoken != null) sb.append(spoken) else sb.append(char)
                }
                "most" -> {
                    if (char != '.' && char != ',' && punctMap.containsKey(char)) {
                        sb.append(punctMap[char])
                    } else {
                        sb.append(char)
                    }
                }
                "some" -> {
                    val criticalSymbols = setOf('@', '#', '$', '%', '&', '*', '=', '/', '\\', '<', '>')
                    if (char in criticalSymbols && punctMap.containsKey(char)) {
                        sb.append(punctMap[char])
                    } else {
                        sb.append(char)
                    }
                }
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }
}
