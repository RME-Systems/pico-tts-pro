package com.rme.picottspro

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference

class SingleChoiceListPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    private val entries = (1..100).map { it.toString() }.toTypedArray()

    var value: Int = 50
        set(newValue) {
            field = newValue
            persistInt(newValue)
            summary = newValue.toString()
        }

    init {
        summary = value.toString()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val def = (defaultValue as? Int) ?: 50
        value = getPersistedInt(def)
    }

    override fun onClick() {
        super.onClick()
        showSelectionDialog()
    }

    private fun showSelectionDialog() {
        val currentIndex = (value - 1).coerceIn(0, entries.size - 1)

        var dialog: AlertDialog? = null
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(entries, currentIndex) { d, which ->
                val selectedNumber = which + 1
                if (callChangeListener(selectedNumber)) {
                    value = selectedNumber
                }
                // Automatically dismiss and return to previous screen instantly
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)

        dialog = builder.create()
        dialog.show()
    }
}
