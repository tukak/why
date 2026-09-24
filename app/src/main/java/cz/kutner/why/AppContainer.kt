package cz.kutner.why

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.WhyDatabase
import cz.kutner.why.data.settings.SettingsRepository
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.ReasonColor
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock: Clock = Clock.systemDefaultZone()

    private val database: WhyDatabase =
        Room.databaseBuilder(appContext, WhyDatabase::class.java, "why.db")
            .setDriver(AndroidSQLiteDriver())
            .build()

    val unlocks = UnlockRepository(database.reasonDao(), database.unlockDao(), database.offerDao())

    val settings = SettingsRepository(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") },
    )

    fun defaultReasons(): List<Reason> = listOf(
        R.string.reason_message to (PebbleShape.Squircle to ReasonColor.Blue),
        R.string.reason_look_up to (PebbleShape.Clover to ReasonColor.Violet),
        R.string.reason_calendar to (PebbleShape.Pill to ReasonColor.Green),
        R.string.reason_music to (PebbleShape.Flower to ReasonColor.Pink),
        R.string.reason_navigate to (PebbleShape.Triangle to ReasonColor.Teal),
        R.string.reason_photo to (PebbleShape.Circle to ReasonColor.Amber),
    ).map { (label, style) ->
        // Navigating needs the phone for the whole trip; a check-in would only interrupt it.
        Reason(label = appContext.getString(label), shape = style.first.name, color = style.second.name, nudge = label != R.string.reason_navigate)
    }
}
