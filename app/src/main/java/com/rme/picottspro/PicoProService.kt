package com.rme.picottspro

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.svox.pico.SynthProxy
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

class PicoProService : TextToSpeechService() {

    companion object {
        private const val TAG = "PicoProService"
        private val SVOX_LANGPACK_RESOURCE = R.raw.svoxlangpack

        private val REQUIRED_VOICE_FILES = arrayOf(
            "de-DE_gl0_sg.bin", "de-DE_ta.bin", "en-GB_kh0_sg.bin", "en-GB_ta.bin",
            "en-US_lh0_sg.bin", "en-US_ta.bin", "es-ES_ta.bin", "es-ES_zl0_sg.bin",
            "fr-FR_nk0_sg.bin", "fr-FR_ta.bin", "it-IT_cm0_sg.bin", "it-IT_ta.bin"
        )
    }

    private var synthProxy: SynthProxy? = null
    private var currentLanguage = "spa"
    private var currentCountry = "ESP"
    private var currentVariant = ""

    private lateinit var settings: SettingsManager
    private lateinit var textProcessor: TextProcessor

    override fun onCreate() {
        settings = SettingsManager(this)
        textProcessor = TextProcessor(this, settings)

        ensureVoiceDataInstalled()
        initEngine()

        // Sync initial language
        val defaultVoiceSetting = settings.defaultVoice
        val parts = defaultVoiceSetting.split("-")
        val lang = parts.getOrNull(0) ?: "spa"
        val country = parts.getOrNull(1) ?: "ESP"

        synthProxy?.setLanguage(lang, country, "")
        currentLanguage = lang
        currentCountry = country

        super.onCreate()
    }

    override fun onDestroy() {
        synthProxy?.stopSync()
        synthProxy?.shutdown()
        synthProxy = null
        super.onDestroy()
    }

    private fun initEngine() {
        if (synthProxy != null) {
            synthProxy?.stopSync()
            synthProxy?.shutdown()
            synthProxy = null
        }
        val config = filesDir.absolutePath + "/"
        try {
            synthProxy = SynthProxy(config)
            Log.i(TAG, "PicoPro synthProxy initialized successfully with config=$config")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize SynthProxy: ${e.message}", e)
        }
    }

    private fun ensureVoiceDataInstalled() {
        val dir = filesDir ?: return
        var allPresent = dir.exists()
        if (allPresent) {
            for (fName in REQUIRED_VOICE_FILES) {
                val f = File(dir, fName)
                if (!f.exists() || f.length() == 0L) {
                    allPresent = false
                    break
                }
            }
        }
        if (allPresent) {
            return
        }
        dir.mkdirs()
        try {
            resources.openRawResource(SVOX_LANGPACK_RESOURCE).use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    val buffer = ByteArray(16384)
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val outFile = File(dir, entry.name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                var count: Int
                                while (zip.read(buffer).also { count = it } != -1) {
                                    fos.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            Log.i(TAG, "Voice data unpacked successfully into ${dir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting voice data: ${e.message}", e)
        }
    }

    private fun normalizeLanguage(lang: String?): String {
        if (lang.isNullOrEmpty()) return "eng"
        val l = lang.lowercase(Locale.ROOT)
        return when (l) {
            "es", "spa" -> "spa"
            "en", "eng" -> "eng"
            "fr", "fra" -> "fra"
            "de", "deu" -> "deu"
            "it", "ita" -> "ita"
            else -> "eng"
        }
    }

    private fun normalizeCountry(langNorm: String, country: String?): String {
        val c = country?.uppercase(Locale.ROOT) ?: ""
        return when (langNorm) {
            "spa" -> "ESP"
            "fra" -> "FRA"
            "deu" -> "DEU"
            "ita" -> "ITA"
            "eng" -> if (c == "GB" || c == "GBR") "GBR" else "USA"
            else -> "USA"
        }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val proxy = synthProxy ?: return TextToSpeech.LANG_NOT_SUPPORTED
        return proxy.isLanguageAvailable(lang ?: "", country ?: "", variant ?: "")
    }

    override fun onGetLanguage(): Array<String>? {
        val proxy = synthProxy ?: return null
        val l = proxy.language
        if (l != null && l.size >= 3 && !l[0].isNullOrEmpty()) {
            return l
        }
        val defaultLoc = Locale.getDefault()
        val iso3Lang = try { defaultLoc.isO3Language } catch (_: Exception) { "eng" }
        val iso3Country = try { defaultLoc.isO3Country } catch (_: Exception) { "USA" }
        if (onIsLanguageAvailable(iso3Lang, iso3Country, "") >= TextToSpeech.LANG_AVAILABLE) {
            return arrayOf(iso3Lang, iso3Country, "")
        }
        return arrayOf("eng", "USA", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val proxy = synthProxy ?: return TextToSpeech.LANG_NOT_SUPPORTED
        val result = onIsLanguageAvailable(lang, country, variant)
        if (result >= TextToSpeech.LANG_AVAILABLE) {
            proxy.setLanguage(lang ?: "", country ?: "", variant ?: "")
        } else {
            proxy.setLanguage("eng", "USA", "")
        }
        return result
    }

    override fun onStop() {
        synthProxy?.stop()
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        val proxy = synthProxy
        if (proxy == null || request == null || callback == null) {
            callback?.error()
            return
        }

        val rawText = request.charSequenceText?.toString() ?: ""
        if (rawText.isEmpty()) {
            return
        }

        // 1. Language handling with safe fallback
        val reqLang = request.language ?: ""
        val reqCountry = request.country ?: ""
        val reqVariant = request.variant ?: ""

        if (proxy.setLanguage(reqLang, reqCountry, reqVariant) != TextToSpeech.SUCCESS) {
            val normLang = normalizeLanguage(reqLang)
            val normCountry = normalizeCountry(normLang, reqCountry)
            if (proxy.setLanguage(normLang, normCountry, "") != TextToSpeech.SUCCESS) {
                if (proxy.setLanguage("eng", "USA", "") != TextToSpeech.SUCCESS) {
                    callback.error()
                    return
                }
            }
        }

        // 2. Speech Rate Override (1 to 100 maps to 20 to 350 for Pico)
        val speechRate = if (settings.isForceRate) {
            val rate100 = settings.forcedRateValue // 1..100
            (rate100 * 3)
        } else {
            request.speechRate
        }
        proxy.setSpeechRate(speechRate)

        // 3. Pitch Override (1 to 100)
        val pitch = if (settings.isForcePitch) {
            val pitch100 = settings.forcedPitchValue // 1..100
            (pitch100 * 2)
        } else {
            request.pitch
        }
        proxy.setPitch(pitch)

        // 4. Emojis and Punctuation Processing
        val activeLocale = Locale.getDefault()
        val processedText = textProcessor.process(rawText, activeLocale)

        // 5. Synthesize
        if (proxy.speakText(processedText, callback) != TextToSpeech.SUCCESS) {
            callback.error()
        }
    }
}
