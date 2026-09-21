package com.freshveg.app.core.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.langDataStore by preferencesDataStore(name = "language_prefs")

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    HINDI("hi", "हिन्दी", "🇮🇳")
}

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_LANG = stringPreferencesKey("app_language_code")
        var instance: LanguageManager? = null
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    private val initialLangCode = prefs.getString("app_language_code", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
    private val _currentLanguage = MutableStateFlow(if (initialLangCode == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        instance = this
        val initialLocale = java.util.Locale(_currentLanguage.value.code)
        java.util.Locale.setDefault(initialLocale)
        scope.launch {
            try {
                val dsPrefs = context.langDataStore.data.first()
                val code = dsPrefs[KEY_LANG] ?: initialLangCode
                val lang = if (code == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH
                _currentLanguage.value = lang
                val loc = java.util.Locale(lang.code)
                java.util.Locale.setDefault(loc)
            } catch (_: Exception) {
                // Keep initial value
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        val loc = java.util.Locale(language.code)
        java.util.Locale.setDefault(loc)
        prefs.edit().putString("app_language_code", language.code).commit()
        scope.launch {
            try {
                context.langDataStore.edit { p ->
                    p[KEY_LANG] = language.code
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleLanguage() {
        val next = if (_currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun t(englishText: String, hindiText: String): String {
        return if (_currentLanguage.value == AppLanguage.HINDI) hindiText else englishText
    }
}
