package cz.kutner.why.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import cz.kutner.why.domain.Trick
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
    val annoyingMode: Boolean = false,
    /** Tricks the user turned off; all others are on while [annoyingMode] is on. */
    val tricksOff: Set<Trick> = emptySet(),
    /** Tricks whose first-time hint was already shown. */
    val tricksSeen: Set<Trick> = emptySet(),
    /** Annoying mode is not offered before this time; [Long.MAX_VALUE] never offers it. */
    val annoyOfferAfter: Long = 0,
) {
    val tricksOn: Set<Trick> get() = if (annoyingMode) Trick.entries.toSet() - tricksOff else emptySet()
}

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
            annoyingMode = p[ANNOYING_MODE] ?: d.annoyingMode,
            tricksOff = p[TRICKS_OFF].toTricks(),
            tricksSeen = p[TRICKS_SEEN].toTricks(),
            annoyOfferAfter = p[ANNOY_OFFER_AFTER] ?: d.annoyOfferAfter,
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

    /** Turning it on starts with every trick on. A user who chose either way knows the mode, so it is not offered again. */
    suspend fun setAnnoyingMode(on: Boolean) = store.edit {
        it[ANNOYING_MODE] = on
        it[TRICKS_OFF] = emptySet()
        it[ANNOY_OFFER_AFTER] = Long.MAX_VALUE
    }

    /** With the last trick turned off, the mode turns off. */
    suspend fun setTrickOn(trick: Trick, on: Boolean) = store.edit {
        val off = it[TRICKS_OFF].toTricks().let { set -> if (on) set - trick else set + trick }
        if (off.size == Trick.entries.size) {
            it[ANNOYING_MODE] = false
            it[TRICKS_OFF] = emptySet()
        } else {
            it[TRICKS_OFF] = off.mapTo(HashSet()) { t -> t.name }
        }
    }

    suspend fun markTrickSeen(trick: Trick) = store.edit { it[TRICKS_SEEN] = (it[TRICKS_SEEN] ?: emptySet()) + trick.name }
    suspend fun setAnnoyOfferAfter(epochMillis: Long) = store.edit { it[ANNOY_OFFER_AFTER] = epochMillis }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val NUDGE_MINUTES = intPreferencesKey("nudge_minutes")
        val RESUME_SECONDS = intPreferencesKey("resume_seconds")
        val PAUSED_UNTIL = longPreferencesKey("paused_until")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val REFLECTION_ENABLED = booleanPreferencesKey("reflection_enabled")
        val REFLECTION_MINUTE = intPreferencesKey("reflection_minute")
        val ANNOYING_MODE = booleanPreferencesKey("annoying_mode")
        val TRICKS_OFF = stringSetPreferencesKey("tricks_off")
        val TRICKS_SEEN = stringSetPreferencesKey("tricks_seen")
        val ANNOY_OFFER_AFTER = longPreferencesKey("annoy_offer_after")

        fun Set<String>?.toTricks(): Set<Trick> = orEmpty().mapNotNullTo(HashSet()) { Trick.of(it) }
    }
}
