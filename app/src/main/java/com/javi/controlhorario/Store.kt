package com.javi.controlhorario

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Entry(val start: Long, val end: Long?)

data class Settings(
    val to: String = "javiermarcosfernandez@gmail.com",
    val smtpUser: String = "javiermarcosfernandez@gmail.com",
    val smtpPass: String = "",
    val hour: Int = 22
)

class Store(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("control_horario", Context.MODE_PRIVATE)

    @Synchronized
    fun entries(): List<Entry> {
        val arr = JSONArray(prefs.getString("entries", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Entry(o.getLong("s"), if (o.isNull("e")) null else o.getLong("e"))
        }
    }

    @Synchronized
    private fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("s", it.start).put("e", it.end ?: JSONObject.NULL)) }
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    fun active(): Entry? = entries().lastOrNull { it.end == null }

    /** Ficha entrada si no hay fichaje abierto; si lo hay, ficha salida. */
    @Synchronized
    fun toggle(now: Long = System.currentTimeMillis()): List<Entry> {
        val list = entries().toMutableList()
        val idx = list.indexOfLast { it.end == null }
        if (idx >= 0) list[idx] = list[idx].copy(end = now) else list.add(Entry(now, null))
        save(list)
        return list
    }

    fun settings() = Settings(
        to = prefs.getString("to", Settings().to)!!,
        smtpUser = prefs.getString("smtpUser", Settings().smtpUser)!!,
        smtpPass = prefs.getString("smtpPass", "")!!,
        hour = prefs.getInt("hour", 22)
    )

    fun saveSettings(s: Settings) {
        prefs.edit().putString("to", s.to).putString("smtpUser", s.smtpUser)
            .putString("smtpPass", s.smtpPass).putInt("hour", s.hour).apply()
    }
}
