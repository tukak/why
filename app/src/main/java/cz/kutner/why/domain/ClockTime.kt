package cz.kutner.why.domain

import java.util.Locale

/** Parses a 24-hour clock time written as H:MM or HH:MM; null when it is not a valid time. */
fun parseMinuteOfDay(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    if (parts[0].isEmpty() || parts[0].length > 2 || parts[1].length != 2) return null
    if (!parts[0].all { it.isDigit() } || !parts[1].all { it.isDigit() }) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

/** Formats minutes after midnight as HH:MM, wrapping around so 24:00 becomes 00:00. */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val wrapped = ((minuteOfDay % MINUTES_IN_DAY) + MINUTES_IN_DAY) % MINUTES_IN_DAY
    return String.format(Locale.ROOT, "%02d:%02d", wrapped / 60, wrapped % 60)
}

const val MINUTES_IN_DAY = 24 * 60
