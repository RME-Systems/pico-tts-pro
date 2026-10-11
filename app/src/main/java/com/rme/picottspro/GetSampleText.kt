package com.rme.picottspro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/**
 * Returns the sample text string for the language requested by Android System TTS Settings.
 * Modern Android (especially Android 14/15) requires this activity to test and preview voices.
 */
class GetSampleText : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var result = TextToSpeech.LANG_AVAILABLE
        val returnData = Intent()

        val extras = intent?.extras
        val language = extras?.getString("language")
        val country = extras?.getString("country")

        when (language) {
            "eng" -> {
                if ("GBR" == country) {
                    returnData.putExtra("sampleText", getString(R.string.eng_gbr_sample))
                } else {
                    returnData.putExtra("sampleText", getString(R.string.eng_usa_sample))
                }
            }
            "fra" -> {
                returnData.putExtra("sampleText", getString(R.string.fra_fra_sample))
            }
            "ita" -> {
                returnData.putExtra("sampleText", getString(R.string.ita_ita_sample))
            }
            "deu" -> {
                returnData.putExtra("sampleText", getString(R.string.deu_deu_sample))
            }
            "spa" -> {
                returnData.putExtra("sampleText", getString(R.string.spa_esp_sample))
            }
            else -> {
                result = TextToSpeech.LANG_NOT_SUPPORTED
                returnData.putExtra("sampleText", "")
            }
        }

        setResult(result, returnData)
        finish()
    }
}
