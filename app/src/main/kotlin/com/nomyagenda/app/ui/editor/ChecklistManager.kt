package com.nomyagenda.app.ui.editor

import com.nomyagenda.app.data.local.entity.ChecklistItem
import org.json.JSONArray
import org.json.JSONObject

object ChecklistManager {

    fun toJson(items: List<ChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("text", item.text)
                put("done", item.done)
            })
        }
        return array.toString()
    }

    fun fromJson(json: String): MutableList<ChecklistItem> {
        val result = mutableListOf<ChecklistItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(ChecklistItem(text = obj.getString("text"), done = obj.getBoolean("done")))
            }
        } catch (e: Exception) { /* ignore malformed JSON */ }
        return result
    }
}
