package com.rme.picottspro

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    // Voice overrides
    val isForceRate: Boolean
        get() = prefs.getBoolean("force_rate", false)

    val forcedRateValue: Int
        get() = prefs.getInt("forced_rate_value", 50) // 1 to 100

    val isForcePitch: Boolean
        get() = prefs.getBoolean("force_pitch", false)

    val forcedPitchValue: Int
        get() = prefs.getInt("forced_pitch_value", 50) // 1 to 100

    val isForceVolume: Boolean
        get() = prefs.getBoolean("force_volume", false)

    val forcedVolumeValue: Int
        get() = prefs.getInt("forced_volume_value", 100) // 1 to 100

    val isForceVoice: Boolean
        get() = prefs.getBoolean("force_voice", false)

    val defaultVoice: String
        get() = prefs.getString("default_voice", "spa-ESP") ?: "spa-ESP"

    // General: Emojis, Punctuation & Numbers
    val isReadEmojis: Boolean
        get() = prefs.getBoolean("read_emojis", true)

    val emojiLanguage: String
        get() = prefs.getString("emoji_language", "auto") ?: "auto"

    val isProcessPunctuation: Boolean
        get() = prefs.getBoolean("process_punctuation", false)

    val punctuationLevel: String
        get() = prefs.getString("punctuation_level", "some") ?: "some"

    val isProcessNumbers: Boolean
        get() = prefs.getBoolean("process_numbers", false)

    val numberMode: Int
        get() = (prefs.getString("number_mode", "3") ?: "3").toIntOrNull() ?: 3

    // User Dictionary
    val isUserDictionaryEnabled: Boolean
        get() = prefs.getBoolean("user_dictionary_enabled", false)

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
