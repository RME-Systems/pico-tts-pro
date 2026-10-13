package com.rme.picottspro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Ajustes de Pico TTS Pro
        findViewById<LinearLayout>(R.id.cardPicoSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 2. Configuración TTS del sistema
        findViewById<LinearLayout>(R.id.cardSystemSettings).setOnClickListener {
            openSystemTtsSettings()
        }

        // 3. Acerca de
        findViewById<LinearLayout>(R.id.cardAbout).setOnClickListener {
            showAboutDialog()
        }
    }

    private fun openSystemTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(fallbackIntent)
        }
    }

    private fun showAboutDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        val btnTelegram = view.findViewById<Button>(R.id.btnTelegram)
        val btnGitHub = view.findViewById<Button>(R.id.btnGitHub)

        val iconSize = (24 * resources.displayMetrics.density).toInt()
        val iconPadding = (12 * resources.displayMetrics.density).toInt()

        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_telegram)?.let { d ->
            d.setBounds(0, 0, iconSize, iconSize)
            btnTelegram.setCompoundDrawables(d, null, null, null)
            btnTelegram.compoundDrawablePadding = iconPadding
        }

        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_github)?.let { d ->
            d.setBounds(0, 0, iconSize, iconSize)
            btnGitHub.setCompoundDrawables(d, null, null, null)
            btnGitHub.compoundDrawablePadding = iconPadding
        }

        btnTelegram.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/rmesystems"))
                startActivity(intent)
            } catch (_: Exception) {}
        }

        view.findViewById<Button>(R.id.btnGitHub).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RME-Systems/pico-tts-pro"))
                startActivity(intent)
            } catch (_: Exception) {}
        }

        view.findViewById<Button>(R.id.btnAccept).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
