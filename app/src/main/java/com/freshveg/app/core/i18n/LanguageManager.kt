package com.freshveg.app.core.i18n

import android.content.Context
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
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        instance = this
        scope.launch {
            try {
                val prefs = context.langDataStore.data.first()
                val code = prefs[KEY_LANG] ?: AppLanguage.ENGLISH.code
                val lang = if (code == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH
                _currentLanguage.value = lang
            } catch (e: Exception) {
                _currentLanguage.value = AppLanguage.ENGLISH
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        scope.launch {
            context.langDataStore.edit { prefs ->
                prefs[KEY_LANG] = language.code
            }
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
