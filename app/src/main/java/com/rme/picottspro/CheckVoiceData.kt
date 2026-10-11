package com.rme.picottspro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.ArrayList

class CheckVoiceData : Activity() {

    companion object {
        private val SUPPORTED_VOICES = arrayListOf(
            "spa-ESP",
            "eng-USA",
            "eng-GBR",
            "fra-FRA",
            "deu-DEU",
            "ita-ITA"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val returnVal = Intent().apply {
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, SUPPORTED_VOICES)
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, ArrayList<String>())
        }
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, returnVal)
        finish()
    }
}
