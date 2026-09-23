package cz.kutner.why.ui

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import cz.kutner.why.R
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.Answer
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

val Reason.style: PebbleStyle get() = PebbleStyle(PebbleShape.of(shape), ReasonColor.of(color))

fun styleOf(answer: Answer, byId: Map<Long, Reason>): PebbleStyle = when (answer) {
    Answer.Habit -> PebbleStyle.Habit
    Answer.Other -> PebbleStyle.Other
    Answer.None -> PebbleStyle.Unanswered
    is Answer.Picked -> byId[answer.reasonId]?.style ?: PebbleStyle.Other
}

/** Emits now and every minute, so live stats move forward while a screen is open. */
fun minuteTicker(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1.minutes)
    }
}

/** Clock time in the phone's 12/24-hour setting; plain locale formats ignore that setting. */
fun timeFormatter(context: Context, locale: Locale = context.resources.configuration.locales[0], hourOnly: Boolean = false): DateTimeFormatter {
    val skeleton = (if (DateFormat.is24HourFormat(context)) "H" else "h") + if (hourOnly) "" else "m"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
fun formatDuration(millis: Long): String = formatDuration(LocalResources.current, millis)

fun formatDuration(res: Resources, millis: Long): String {
    if (millis in 1 until 60_000) return res.getString(R.string.duration_under_minute)
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) res.getString(R.string.duration_hours_minutes, hours, minutes) else res.getString(R.string.duration_minutes, minutes)
}

@Composable
fun formatAverage(millis: Long): String = stringResource(R.string.duration_decimal_minutes, millis / 60_000.0)
