package com.rme.picottspro

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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

    val isReadEmojis: Boolean
        get() = prefs.getBoolean("read_emojis", true)

    val emojiLanguage: String
        get() = prefs.getString("emoji_language", "auto") ?: "auto"

    val punctuationLevel: String
        get() = prefs.getString("punctuation_level", "none") ?: "none"

    val defaultVoice: String
        get() = prefs.getString("default_voice", "spa-ESP") ?: "spa-ESP"
}
