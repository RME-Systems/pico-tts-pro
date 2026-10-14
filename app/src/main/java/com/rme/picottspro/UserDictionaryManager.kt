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
    @Volatile
    private var lastFileTimestamp = 0L

    init {
        load()
    }

    @Synchronized
    private fun checkReload() {
        if (!dictFile.exists()) {
            if (entries.isNotEmpty()) {
                entries.clear()
                lastFileTimestamp = 0L
            }
            return
        }
        val fileTime = dictFile.lastModified()
        if (fileTime != lastFileTimestamp) {
            load()
        }
    }

    @Synchronized
    fun getEntries(): List<DictionaryEntry> {
        checkReload()
        return ArrayList(entries)
    }

    @Synchronized
    fun addEntry(word: String, replacement: String) {
        checkReload()
        val w = word.trim()
        val r = replacement.trim()
        if (w.isEmpty()) return
        // Remove existing duplicate if present
        entries.removeAll { it.word.equals(w, ignoreCase = true) }
        entries.add(DictionaryEntry(word = w, replacement = r))
        save()
    }

    @Synchronized
    fun updateEntry(id: String, newWord: String, newReplacement: String) {
        checkReload()
        val w = newWord.trim()
        val r = newReplacement.trim()
        if (w.isEmpty()) return
        entries.removeAll { it.id != id && it.word.equals(w, ignoreCase = true) }
        val idx = entries.indexOfFirst { it.id == id }
        if (idx != -1) {
            entries[idx] = DictionaryEntry(id = id, word = w, replacement = r)
        } else {
            entries.add(DictionaryEntry(id = id, word = w, replacement = r))
        }
        save()
    }

    @Synchronized
    fun removeEntry(id: String) {
        checkReload()
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
        checkReload()
        if (text.isEmpty() || entries.isEmpty()) return text
        var result = text
        // Longer words first to avoid substring partial overrides
        val sorted = entries.sortedByDescending { it.word.length }
        for (entry in sorted) {
            val w = entry.word.trim()
            if (w.isEmpty()) continue
            val startsWithWord = Character.isLetterOrDigit(w.first()) || w.first() == '_'
            val endsWithWord = Character.isLetterOrDigit(w.last()) || w.last() == '_'

            val leftBoundary = if (startsWithWord) "(?<![\\p{L}\\p{N}_])" else ""
            val rightBoundary = if (endsWithWord) "(?![\\p{L}\\p{N}_])" else ""

            val pattern = "(?i)$leftBoundary" + java.util.regex.Pattern.quote(w) + rightBoundary
            try {
                result = result.replace(Regex(pattern), entry.replacement)
            } catch (e: Exception) {
                result = result.replace(w, entry.replacement, ignoreCase = true)
            }
        }
        return result
    }

    @Synchronized
    fun exportToJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("name", "PicoTTS Dictionary")
        root.put("enabled", true)
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("word", entry.word)
            obj.put("source", entry.word)
            obj.put("replacement", entry.replacement)
            obj.put("matchMode", "Exact match")
            obj.put("caseSensitive", false)
            obj.put("type", "text")
            arr.put(obj)
        }
        root.put("entries", arr)
        return root.toString(2)
    }

    @Synchronized
    fun importEntries(content: String): Int {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return 0
        var importedCount = 0
        val newEntries = mutableListOf<Pair<String, String>>()

        try {
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (root.has("entries")) {
                    val arr = root.getJSONArray("entries")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val w = if (obj.has("source")) obj.getString("source") else obj.optString("word", "")
                        val r = obj.optString("replacement", "")
                        if (w.trim().isNotEmpty()) {
                            newEntries.add(w.trim() to r.trim())
                        }
                    }
                } else {
                    // Key-value map format: { "key": "value" }
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = root.optString(k, "")
                        if (k.trim().isNotEmpty()) {
                            newEntries.add(k.trim() to v.trim())
                        }
                    }
                }
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.get(i)
                    if (item is JSONObject) {
                        val w = if (item.has("source")) item.getString("source") else item.optString("word", "")
                        val r = item.optString("replacement", "")
                        if (w.trim().isNotEmpty()) {
                            newEntries.add(w.trim() to r.trim())
                        }
                    }
                }
            } else {
                // Line-based (TSV / .dic / key=val)
                trimmed.lines().forEach { line ->
                    val l = line.trim()
                    if (l.isNotEmpty() && !l.startsWith("#") && !l.startsWith("//")) {
                        val parts = when {
                            l.contains("\t") -> l.split("\t", limit = 2)
                            l.contains("=") -> l.split("=", limit = 2)
                            l.contains(":") -> l.split(":", limit = 2)
                            else -> null
                        }
                        if (parts != null && parts.size >= 2) {
                            val w = parts[0].trim()
                            val r = parts[1].trim()
                            if (w.isNotEmpty()) {
                                newEntries.add(w to r)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserDict", "Error parsing import content: ${e.message}")
            if (newEntries.isEmpty()) {
                trimmed.lines().forEach { line ->
                    val l = line.trim()
                    if (l.isNotEmpty() && !l.startsWith("#") && !l.startsWith("//")) {
                        val parts = when {
                            l.contains("\t") -> l.split("\t", limit = 2)
                            l.contains("=") -> l.split("=", limit = 2)
                            else -> null
                        }
                        if (parts != null && parts.size >= 2) {
                            val w = parts[0].trim()
                            val r = parts[1].trim()
                            if (w.isNotEmpty()) {
                                newEntries.add(w to r)
                            }
                        }
                    }
                }
            }
        }

        if (newEntries.isNotEmpty()) {
            for ((word, replacement) in newEntries) {
                entries.removeAll { it.word.equals(word, ignoreCase = true) }
                entries.add(DictionaryEntry(word = word, replacement = replacement))
                importedCount++
            }
            save()
        }
        return importedCount
    }

    @Synchronized
    private fun load() {
        entries.clear()
        if (!dictFile.exists()) {
            lastFileTimestamp = 0L
            return
        }
        try {
            lastFileTimestamp = dictFile.lastModified()
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
            lastFileTimestamp = dictFile.lastModified()
        } catch (e: Exception) {
            android.util.Log.e("UserDict", "Error saving dictionary: ${e.message}")
        }
    }
}
