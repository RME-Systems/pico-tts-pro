package com.rme.picottspro

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Preprocessor that transforms numbers into Digits, Pairs, Triplets, or Smart mode.
 * Adaptado para Pico TTS Pro.
 */
object NumberProcessor {

    const val MODE_DIGITS = 0
    const val MODE_PAIRS = 1
    const val MODE_TRIPLETS = 2
    const val MODE_SMART = 3

    private val NUMBER_REGEX = Regex("""\d+""")

    private val CURRENCY_PATTERN = Pattern.compile("([$€£¥₹¢]|\\b(?:USD|EUR|GBP|INR|Rs\\.?)\\s*)\\d+(?:[.,]\\d+)?")
    private val PERCENT_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?\\s*%")
    private val DECIMAL_PATTERN = Pattern.compile("\\b\\d+\\.\\d+\\b")
    private val COMMA_NUM_PATTERN = Pattern.compile("\\b\\d{1,3}(?:,\\d{3})+\\b")
    private val DATE_PATTERN = Pattern.compile("\\b\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}\\b")
    private val TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b")
    private val VERSION_PATTERN = Pattern.compile("\\b(?:version|v\\.|ver\\.|build)\\s+\\d+\\b", Pattern.CASE_INSENSITIVE)
    private val QUANTITY_PATTERN = Pattern.compile(
        "\\b\\d+\\s+(?:manzanas|naranjas|artículos|personas|usuarios|dólares|euros|centavos|millas|km|metros|segundos|minutos|horas|días|semanas|meses|años|por ciento|veces|libros|autos|casas|puntos|pasos|bytes|kb|mb|gb|apples|oranges|items|people|users|dollars|euros|cents|miles|seconds|minutes|hours|days|weeks|months|years|percent|times)\\b",
        Pattern.CASE_INSENSITIVE
    )
    private val ALPHANUMERIC_PATTERN = Pattern.compile("\\b(?=[A-Za-z0-9]*[A-Za-z])(?=[A-Za-z0-9]*\\d)[A-Za-z0-9]+\\b")
    private val OTP_CONTEXT_PATTERN = Pattern.compile(
        "\\b(?:otp|código|codigo|clave|pin|contraseña|contrasena|verificación|verificacion|seguridad|code|passcode|password|verification|security code|verify)\\b",
        Pattern.CASE_INSENSITIVE
    )
    private val DIGIT_RUN_PATTERN = Pattern.compile("\\b\\d+\\b")

    private val FORMATTED_PHONE_PATTERN = Pattern.compile(
        """(?:\+(\d{1,3})\s*)?(?:(?:\((\d{3})\)\s*|(\d{3})[-. ])(\d{3})[-. ](\d{4})|(\d{5})[-. ](\d{5}))\b"""
    )

    fun process(
        text: String,
        enabled: Boolean = false,
        mode: Int = MODE_SMART
    ): String {
        if (!enabled || text.isEmpty()) return text

        if (mode == MODE_SMART) {
            return processSmart(text)
        }

        return NUMBER_REGEX.replace(text) { match ->
            formatNumber(match.value, mode)
        }
    }

    fun processSmart(text: String): String {
        if (text.isEmpty()) return text
        val pass1 = processFormattedPhones(text)
        return processDigitRuns(pass1)
    }

    private fun processFormattedPhones(text: String): String {
        val protectedMask = BooleanArray(text.length)
        markPattern(protectedMask, CURRENCY_PATTERN.matcher(text))
        markPattern(protectedMask, PERCENT_PATTERN.matcher(text))
        markPattern(protectedMask, DECIMAL_PATTERN.matcher(text))
        markPattern(protectedMask, COMMA_NUM_PATTERN.matcher(text))
        markPattern(protectedMask, DATE_PATTERN.matcher(text))
        markPattern(protectedMask, TIME_PATTERN.matcher(text))
        markPattern(protectedMask, VERSION_PATTERN.matcher(text))
        markPattern(protectedMask, QUANTITY_PATTERN.matcher(text))
        markPattern(protectedMask, ALPHANUMERIC_PATTERN.matcher(text))

        val m = FORMATTED_PHONE_PATTERN.matcher(text)
        val sb = StringBuilder(text.length + 16)
        var lastIdx = 0

        while (m.find()) {
            val start = m.start()
            val end = m.end()

            var isProtected = false
            for (k in start until end) {
                if (protectedMask[k]) {
                    isProtected = true
                    break
                }
            }

            sb.append(text, lastIdx, start)
            if (isProtected) {
                sb.append(text, start, end)
            } else {
                val country = m.group(1)
                val areaParen = m.group(2)
                val areaPlain = m.group(3)
                val exchange = m.group(4)
                val line = m.group(5)
                val five1 = m.group(6)
                val five2 = m.group(7)

                val phoneDigits = StringBuilder()
                if (country != null) {
                    phoneDigits.append("+").append(country).append(" ")
                }

                if (exchange != null && line != null) {
                    val area = areaParen ?: areaPlain ?: ""
                    phoneDigits.append(toSpacedDigits(area))
                        .append(", ")
                        .append(toSpacedDigits(exchange))
                        .append(", ")
                        .append(toSpacedDigits(line))
                } else if (five1 != null && five2 != null) {
                    phoneDigits.append(toSpacedDigits(five1))
                        .append(", ")
                        .append(toSpacedDigits(five2))
                } else {
                    phoneDigits.append(text, start, end)
                }
                sb.append(phoneDigits)
            }
            lastIdx = end
        }
        sb.append(text.substring(lastIdx))
        return sb.toString()
    }

    private fun processDigitRuns(text: String): String {
        val protectedMask = BooleanArray(text.length)
        markPattern(protectedMask, CURRENCY_PATTERN.matcher(text))
        markPattern(protectedMask, PERCENT_PATTERN.matcher(text))
        markPattern(protectedMask, DECIMAL_PATTERN.matcher(text))
        markPattern(protectedMask, COMMA_NUM_PATTERN.matcher(text))
        markPattern(protectedMask, DATE_PATTERN.matcher(text))
        markPattern(protectedMask, TIME_PATTERN.matcher(text))
        markPattern(protectedMask, VERSION_PATTERN.matcher(text))
        markPattern(protectedMask, QUANTITY_PATTERN.matcher(text))
        markPattern(protectedMask, ALPHANUMERIC_PATTERN.matcher(text))

        val m = DIGIT_RUN_PATTERN.matcher(text)
        val result = StringBuilder(text.length + 16)
        var lastIdx = 0

        while (m.find()) {
            val start = m.start()
            val end = m.end()
            result.append(text, lastIdx, start)

            var isProtected = false
            for (k in start until end) {
                if (protectedMask[k]) {
                    isProtected = true
                    break
                }
            }

            val digits = m.group()
            if (isProtected) {
                result.append(digits)
            } else {
                val len = digits.length
                val windowStart = (start - 30).coerceAtLeast(0)
                val windowEnd = (end + 30).coerceAtMost(text.length)
                val window = text.substring(windowStart, windowEnd)
                val isOtp = OTP_CONTEXT_PATTERN.matcher(window).find()

                if (isOtp && len in 4..8) {
                    result.append(formatOtp(digits))
                } else if (len < 7) {
                    result.append(digits)
                } else {
                    result.append(formatSmartDigits(digits))
                }
            }
            lastIdx = end
        }
        result.append(text.substring(lastIdx))
        return result.toString()
    }

    private fun formatOtp(digits: String): String {
        return when (digits.length) {
            4 -> toSpacedDigits(digits.substring(0, 2)) + ", " + toSpacedDigits(digits.substring(2))
            5 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            6 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            7 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            8 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4))
            else -> toSpacedDigits(digits)
        }
    }

    private fun markPattern(mask: BooleanArray, m: Matcher) {
        while (m.find()) {
            val start = m.start()
            val end = m.end()
            for (k in start until end) {
                mask[k] = true
            }
        }
    }

    private fun formatSmartDigits(digits: String): String {
        val len = digits.length
        return when (len) {
            7 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3))
            8 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4))
            9 -> toSpacedDigits(digits.substring(0, 3)) + ", " + toSpacedDigits(digits.substring(3, 6)) + ", " + toSpacedDigits(digits.substring(6))
            10 -> toSpacedDigits(digits.substring(0, 5)) + ", " + toSpacedDigits(digits.substring(5))
            12 -> toSpacedDigits(digits.substring(0, 4)) + ", " + toSpacedDigits(digits.substring(4, 8)) + ", " + toSpacedDigits(digits.substring(8))
            else -> {
                val sb = StringBuilder()
                val chunkLen = if (len % 3 == 0) 3 else 4
                var idx = 0
                while (idx < len) {
                    val end = (idx + chunkLen).coerceAtMost(len)
                    if (sb.isNotEmpty()) sb.append(", ")
                    sb.append(toSpacedDigits(digits.substring(idx, end)))
                    idx = end
                }
                sb.toString()
            }
        }
    }

    private fun toSpacedDigits(s: String): String {
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0) sb.append(' ')
            sb.append(s[i])
        }
        return sb.toString()
    }

    fun formatNumber(digits: String, mode: Int): String {
        if (digits.length <= 1) return digits
        return when (mode) {
            MODE_DIGITS -> digits.toCharArray().joinToString(" ")
            MODE_PAIRS -> digits.chunked(2).joinToString(" ")
            MODE_TRIPLETS -> {
                val len = digits.length
                val firstGroupLen = if (len % 3 == 0) 3 else len % 3
                val sb = StringBuilder()
                sb.append(digits.substring(0, firstGroupLen))
                var idx = firstGroupLen
                while (idx < len) {
                    sb.append(" ")
                    sb.append(digits.substring(idx, idx + 3))
                    idx += 3
                }
                sb.toString()
            }
            MODE_SMART -> formatSmartDigits(digits)
            else -> digits
        }
    }
}
