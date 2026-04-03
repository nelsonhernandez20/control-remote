package com.controlremote.tv.androidtv

import android.content.Context

/**
 * Orden persistente de dispositivos en la lista (Televisores).
 * Cada clave: "a" + [SEP] + host (Android TV) o "s" + [SEP] + host (Samsung). [SEP] = unit separator.
 */
class DeviceOrderStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrder(): List<String> {
        val raw = prefs.getString(KEY_ORDER, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD_SEP).filter { it.isNotBlank() }
    }

    fun setOrder(keys: List<String>) {
        prefs.edit().putString(KEY_ORDER, keys.joinToString(RECORD_SEP)).apply()
    }

    fun appendIfMissing(key: String) {
        val cur = getOrder().toMutableList()
        if (!cur.contains(key)) {
            cur.add(key)
            setOrder(cur)
        }
    }

    fun removeKey(key: String) {
        setOrder(getOrder().filter { it != key })
    }

    companion object {
        private const val PREFS_NAME = "device_list_order"
        private const val KEY_ORDER = "order"
        /** Separa entradas en la lista guardada */
        private const val RECORD_SEP = "\u001e"
        /** Separa tipo (a/s) y host dentro de una clave */
        private const val FIELD_SEP = "\u001f"

        fun keyAndroid(host: String) = "a$FIELD_SEP${host.trim()}"
        fun keySamsung(host: String) = "s$FIELD_SEP${host.trim()}"
    }
}
