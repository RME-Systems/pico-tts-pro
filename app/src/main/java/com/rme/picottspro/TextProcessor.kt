package com.rme.picottspro

import android.content.Context
import android.util.Log
import java.util.Locale

class TextProcessor(
    private val context: Context,
    private val settings: SettingsManager
) {

    companion object {
        private const val TAG = "TextProcessor"
    }

    private val userDict = UserDictionaryManager(context)

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

    init {
        try {
            EmojiData.ensureLoaded(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed early EmojiData load: ${e.message}", e)
        }
    }

    fun getUserDictionary(): UserDictionaryManager = userDict

    fun process(rawText: String, activeLocale: Locale): String {
        if (rawText.isEmpty()) return rawText
        var text = rawText

        // 1. User Dictionary Replacements
        if (settings.isUserDictionaryEnabled) {
            text = userDict.process(text)
        }

        // 2. Unicode Emojis & ASCII Emoticons (idioma ligado a la voz activa)
        if (settings.isReadEmojis) {
            val targetLang = activeLocale.language
            EmojiData.ensureLoaded(context)
            text = processEmojisWithTrie(text, targetLang)
            text = EmoticonProcessor.process(text, targetLang)
        }

        // 3. Number Processing (Digits, Pairs, Triplets, Smart)
        if (settings.isProcessNumbers) {
            text = NumberProcessor.process(text, enabled = true, mode = settings.numberMode)
        }

        // 4. Punctuation Processing
        if (settings.isProcessPunctuation) {
            val punctLevel = settings.punctuationLevel
            if (punctLevel != "none") {
                text = processPunctuation(text, punctLevel, activeLocale)
            }
        }

        return text
    }

    private fun processEmojisWithTrie(text: String, language: String): String {
        if (text.isEmpty() || !EmojiData.isLoaded) return text

        val langIdx = EmojiData.getLanguageIndex(language)
        val sb = StringBuilder(text.length + 64)
        var i = 0
        var prevWasEmoji = false

        while (i < text.length) {
            val match = EmojiData.findLongestMatch(text, i)
            if (match != null) {
                val (matchLen, ttsArray) = match
                val spoken = ttsArray[langIdx] ?: ttsArray[EmojiData.LANG_EN] ?: ""

                if (spoken.isNotEmpty()) {
                    if (prevWasEmoji) {
                        while (sb.isNotEmpty() && Character.isWhitespace(sb[sb.length - 1])) {
                            sb.deleteCharAt(sb.length - 1)
                        }
                        sb.append(", ")
                    } else if (sb.isNotEmpty()) {
                        val prevChar = sb[sb.length - 1]
                        if (Character.isLetterOrDigit(prevChar)) {
                            sb.append(' ')
                        }
                    }

                    sb.append(spoken)

                    val nextIdx = i + matchLen
                    if (nextIdx < text.length) {
                        val nextChar = text[nextIdx]
                        if (Character.isLetterOrDigit(nextChar)) {
                            sb.append(' ')
                        }
                    }

                    i += matchLen
                    prevWasEmoji = true
                    continue
                }
            }

            val ch = text[i]
            sb.append(ch)
            i++
            if (!Character.isWhitespace(ch)) {
                prevWasEmoji = false
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
