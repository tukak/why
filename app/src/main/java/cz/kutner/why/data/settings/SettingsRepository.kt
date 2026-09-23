package cz.kutner.why.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    /** 0 turns check-ins off. */
    val nudgeMinutes: Int = 10,
    /** An unlock this soon after screen off continues the answered session; 0 always asks. */
    val resumeSeconds: Int = 3,
    val pausedUntil: Long = 0,
    val onboardingDone: Boolean = false,
    val reflectionEnabled: Boolean = true,
    /** Local time of the evening summary, in minutes after midnight. */
    val reflectionMinute: Int = 21 * 60,
)

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
            dynamicColor = p[DYNAMIC_COLOR] ?: d.dynamicColor,
            nudgeMinutes = p[NUDGE_MINUTES] ?: d.nudgeMinutes,
            resumeSeconds = p[RESUME_SECONDS] ?: d.resumeSeconds,
            pausedUntil = p[PAUSED_UNTIL] ?: d.pausedUntil,
            onboardingDone = p[ONBOARDING_DONE] ?: d.onboardingDone,
            reflectionEnabled = p[REFLECTION_ENABLED] ?: d.reflectionEnabled,
            reflectionMinute = p[REFLECTION_MINUTE] ?: d.reflectionMinute,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setNudgeMinutes(minutes: Int) = store.edit { it[NUDGE_MINUTES] = minutes }
    suspend fun setResumeSeconds(seconds: Int) = store.edit { it[RESUME_SECONDS] = seconds }
    suspend fun setPausedUntil(epochMillis: Long) = store.edit { it[PAUSED_UNTIL] = epochMillis }
    suspend fun setOnboardingDone() = store.edit { it[ONBOARDING_DONE] = true }
    suspend fun setReflectionEnabled(enabled: Boolean) = store.edit { it[REFLECTION_ENABLED] = enabled }
    suspend fun setReflectionMinute(minute: Int) = store.edit { it[REFLECTION_MINUTE] = minute }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val NUDGE_MINUTES = intPreferencesKey("nudge_minutes")
        val RESUME_SECONDS = intPreferencesKey("resume_seconds")
        val PAUSED_UNTIL = longPreferencesKey("paused_until")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REFLECTION_ENABLED = booleanPreferencesKey("reflection_enabled")
        val REFLECTION_MINUTE = intPreferencesKey("reflection_minute")
    }
}
