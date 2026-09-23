package cz.kutner.why.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/** Helps the user keep the unlock service alive on phones that stop background apps. */
object BackgroundHelp {

    fun isUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** App info is the one screen every manufacturer keeps, and its Battery entry leads to "Unrestricted". */
    fun openAppSettings(context: Context) =
        open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))

    fun openOverlaySettings(context: Context) =
        open(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))

    fun openManufacturerTips(context: Context) = open(context, Intent(Intent.ACTION_VIEW, tipsUrl().toUri()))

    val manufacturer: String get() = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }

    /** Page names used by dontkillmyapp.com where they differ from [Build.MANUFACTURER]. */
    private val pageNames = mapOf("hmd global" to "nokia", "poco" to "xiaomi", "redmi" to "xiaomi")

    private fun tipsUrl(): String {
        val key = Build.MANUFACTURER.lowercase()
        return "https://dontkillmyapp.com/" + (pageNames[key] ?: key.replace(" ", "-"))
    }

    /** Some phones lack a settings screen or a browser; then the tap does nothing instead of crashing. */
    fun open(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
