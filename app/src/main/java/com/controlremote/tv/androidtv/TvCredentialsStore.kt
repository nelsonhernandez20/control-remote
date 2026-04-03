package com.controlremote.tv.androidtv

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacena certificado y clave PEM del protocolo Android TV por IP (emparejamiento local).
 */
class TvCredentialsStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasCredentials(host: String): Boolean =
        prefs.contains(certKey(host)) && prefs.contains(keyKey(host))

    fun savePem(host: String, certPem: String, keyPem: String) {
        prefs.edit()
            .putString(certKey(host), certPem)
            .putString(keyKey(host), keyPem)
            .apply()
    }

    fun getCertPem(host: String): String? = prefs.getString(certKey(host), null)

    fun getKeyPem(host: String): String? = prefs.getString(keyKey(host), null)

    fun clear(host: String) {
        prefs.edit()
            .remove(certKey(host))
            .remove(keyKey(host))
            .apply()
    }

    private fun certKey(host: String) = "cert_$host"
    private fun keyKey(host: String) = "key_$host"

    companion object {
        private const val PREFS_NAME = "atv_remote_creds"
    }
}
