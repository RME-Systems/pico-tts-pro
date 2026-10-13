package com.rme.picottspro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class DictionaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val replacement: String
)

class UserDictionaryManager(context: Context) {

    private val dictFile = File(context.filesDir, "user_dictionary.json")
    private val entries = ArrayList<DictionaryEntry>()

    init {
        load()
    }

    @Synchronized
    fun getEntries(): List<DictionaryEntry> {
        return ArrayList(entries)
    }

    @Synchronized
    fun addEntry(word: String, replacement: String) {
        val w = word.trim()
        val r = replacement.trim()
        if (w.isEmpty()) return
        // Remove existing duplicate if present
        entries.removeAll { it.word.equals(w, ignoreCase = true) }
        entries.add(DictionaryEntry(word = w, replacement = r))
        save()
    }

    @Synchronized
    fun removeEntry(id: String) {
        entries.removeAll { it.id == id }
        save()
    }

    @Synchronized
    fun clearAll() {
        entries.clear()
        save()
    }

    @Synchronized
    fun process(text: String): String {
        if (text.isEmpty() || entries.isEmpty()) return text
        var result = text
        // Longer words first to avoid substring partial overrides
        val sorted = entries.sortedByDescending { it.word.length }
        for (entry in sorted) {
            val pattern = "(?i)\\b" + java.util.regex.Pattern.quote(entry.word) + "\\b"
            result = result.replace(Regex(pattern), entry.replacement)
        }
        return result
    }

    @Synchronized
    private fun load() {
        entries.clear()
        if (!dictFile.exists()) return
        try {
            val jsonStr = dictFile.readText(Charsets.UTF_8)
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val word = obj.optString("word")
                val rep = obj.optString("replacement")
                val id = obj.optString("id", UUID.randomUUID().toString())
                if (word.isNotEmpty()) {
                    entries.add(DictionaryEntry(id, word, rep))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserDict", "Error loading dictionary: ${e.message}")
        }
    }

    @Synchronized
    private fun save() {
        try {
            val arr = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("id", entry.id)
                obj.put("word", entry.word)
                obj.put("replacement", entry.replacement)
                arr.put(obj)
            }
            dictFile.writeText(arr.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("UserDict", "Error saving dictionary: ${e.message}")
        }
    }
}
