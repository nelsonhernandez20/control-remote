package com.controlremote.tv.androidtv

import android.content.Context

/**
 * IPs Samsung usadas al menos una vez (sin cifrado; solo dirección y nombre opcional).
 */
class SamsungKnownStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listHosts(): List<String> {
        val set = prefs.getStringSet(KEY_HOSTS, null) ?: return emptyList()
        return set.toList().sorted()
    }

    fun rememberHost(ip: String) {
        val host = ip.trim()
        if (host.isEmpty()) return
        val prev = prefs.getStringSet(KEY_HOSTS, emptySet()) ?: emptySet()
        val next = HashSet(prev)
        next.add(host)
        prefs.edit().putStringSet(KEY_HOSTS, next).apply()
    }

    fun setDisplayName(ip: String, name: String) {
        prefs.edit().putString(nameKey(ip), name).apply()
    }

    fun getDisplayName(ip: String): String? = prefs.getString(nameKey(ip), null)

    fun removeHost(ip: String) {
        val prev = prefs.getStringSet(KEY_HOSTS, emptySet()) ?: emptySet()
        val next = HashSet(prev)
        next.remove(ip.trim())
        prefs.edit()
            .putStringSet(KEY_HOSTS, next)
            .remove(nameKey(ip))
            .apply()
    }

    private fun nameKey(ip: String) = "name_${ip.trim()}"

    companion object {
        private const val PREFS_NAME = "samsung_known_tv"
        private const val KEY_HOSTS = "hosts"
    }
}
