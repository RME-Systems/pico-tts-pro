package com.rme.picottspro

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                val ctx = context ?: return@registerForActivityResult
                try {
                    val dictManager = UserDictionaryManager(ctx)
                    val json = dictManager.exportToJson()
                    ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(json)
                        }
                    }
                    Toast.makeText(ctx, R.string.toast_export_success, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, getString(R.string.toast_export_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }

        private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val ctx = context ?: return@registerForActivityResult
                try {
                    val content = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (content != null) {
                        val dictManager = UserDictionaryManager(ctx)
                        val count = dictManager.importEntries(content)
                        if (count > 0) {
                            Toast.makeText(ctx, getString(R.string.toast_import_success, count), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, R.string.toast_import_empty, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(ctx, getString(R.string.toast_import_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Administrar Diccionario
            findPreference<Preference>("manage_dictionary")?.setOnPreferenceClickListener {
                showManageDictionaryDialog(requireContext())
                true
            }

            // Importar Diccionario
            findPreference<Preference>("import_dictionary")?.setOnPreferenceClickListener {
                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                true
            }

            // Exportar Diccionario
            findPreference<Preference>("export_dictionary")?.setOnPreferenceClickListener {
                exportLauncher.launch("pico_user_dictionary.json")
                true
            }

            // Restablecer parámetros a valores de fábrica
            findPreference<Preference>("reset_defaults")?.setOnPreferenceClickListener {
                showResetDefaultsDialog(requireContext())
                true
            }
        }

        private fun showResetDefaultsDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_reset_title)
                .setMessage(R.string.dialog_reset_message)
                .setPositiveButton(R.string.btn_accept) { _, _ ->
                    val sm = SettingsManager(context)
                    sm.resetToDefaults()
                    // Recargar pantalla de preferencias
                    preferenceScreen.removeAll()
                    setPreferencesFromResource(R.xml.root_preferences, null)
                    Toast.makeText(context, R.string.dialog_reset_title, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        private fun showManageDictionaryDialog(context: Context) {
            val dictManager = UserDictionaryManager(context)
            val entries = dictManager.getEntries()

            val items = if (entries.isEmpty()) {
                arrayOf(context.getString(R.string.empty_dictionary), "+ " + context.getString(R.string.dialog_add_word))
            } else {
                entries.map { "${it.word}  ➔  ${it.replacement}" }.toTypedArray() + ("+ " + context.getString(R.string.dialog_add_word))
            }

            AlertDialog.Builder(context)
                .setTitle(R.string.pref_manage_dictionary)
                .setItems(items) { _, which ->
                    if (which == items.size - 1) {
                        // Click en "+ Añadir palabra"
                        showAddWordDialog(context, dictManager)
                    } else if (entries.isNotEmpty()) {
                        // Click en palabra existente para editar o borrar
                        val entry = entries[which]
                        showEditWordDialog(context, dictManager, entry)
                    }
                }
                .setNegativeButton(R.string.btn_accept, null)
                .show()
        }

        private fun showEditWordDialog(context: Context, dictManager: UserDictionaryManager, entry: DictionaryEntry) {
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val inputWord = EditText(context).apply {
                hint = context.getString(R.string.hint_original_word)
                setText(entry.word)
                setSelection(entry.word.length)
            }
            val inputReplacement = EditText(context).apply {
                hint = context.getString(R.string.hint_replacement_word)
                setText(entry.replacement)
                setSelection(entry.replacement.length)
            }

            layout.addView(inputWord)
            layout.addView(inputReplacement)

            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_edit_word)
                .setView(layout)
                .setPositiveButton(R.string.btn_save) { _, _ ->
                    val w = inputWord.text.toString().trim()
                    val r = inputReplacement.text.toString().trim()
                    if (w.isNotEmpty() && r.isNotEmpty()) {
                        dictManager.updateEntry(entry.id, w, r)
                    }
                    showManageDictionaryDialog(context)
                }
                .setNeutralButton(R.string.btn_delete) { _, _ ->
                    dictManager.removeEntry(entry.id)
                    showManageDictionaryDialog(context)
                }
                .setNegativeButton(R.string.btn_cancel) { _, _ ->
                    showManageDictionaryDialog(context)
                }
                .show()
        }

        private fun showAddWordDialog(context: Context, dictManager: UserDictionaryManager) {
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val inputWord = EditText(context).apply {
                hint = context.getString(R.string.hint_original_word)
            }
            val inputReplacement = EditText(context).apply {
                hint = context.getString(R.string.hint_replacement_word)
            }

            layout.addView(inputWord)
            layout.addView(inputReplacement)

            AlertDialog.Builder(context)
                .setTitle(R.string.dialog_add_word)
                .setView(layout)
                .setPositiveButton(R.string.btn_add) { _, _ ->
                    val w = inputWord.text.toString().trim()
                    val r = inputReplacement.text.toString().trim()
                    if (w.isNotEmpty() && r.isNotEmpty()) {
                        dictManager.addEntry(w, r)
                    }
                    showManageDictionaryDialog(context)
                }
                .setNegativeButton(R.string.btn_cancel) { _, _ ->
                    showManageDictionaryDialog(context)
                }
                .show()
        }
    }
}
