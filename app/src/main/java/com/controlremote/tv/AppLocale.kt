package com.controlremote.tv

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Idioma de la app (persistido). Vacío = seguir el idioma del sistema.
 */
object AppLocale {

    private const val PREFS = "app_locale_prefs"
    private const val KEY = "locale_tag"

    const val SYSTEM = ""
    const val EN = "en"
    const val ES = "es"
    const val FR = "fr"
    const val PT = "pt"

    fun applyStoredLocale(context: Context) {
        val tag = prefs(context).getString(KEY, SYSTEM) ?: SYSTEM
        applyLocales(tag)
    }

    fun persistAndApply(context: Context, languageTag: String) {
        prefs(context).edit().putString(KEY, languageTag).apply()
        applyLocales(languageTag)
    }

    fun currentTag(context: Context): String =
        prefs(context).getString(KEY, SYSTEM) ?: SYSTEM

    private fun applyLocales(tag: String) {
        if (tag.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
