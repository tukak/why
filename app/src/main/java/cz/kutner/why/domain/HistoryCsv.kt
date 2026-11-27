package cz.kutner.why.domain

import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.UnlockEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** One row per session. `seconds` is empty while a session is open; times are in [zone]. */
fun writeHistoryCsv(out: Appendable, events: List<UnlockEvent>, reasons: Map<Long, Reason>, zone: ZoneId) {
    out.append("unlocked_at,seconds,answer,reason\n")
    for (event in events) {
        val time = Instant.ofEpochMilli(event.unlockedAt).atZone(zone).truncatedTo(ChronoUnit.SECONDS)
        val seconds = event.lockedAt?.let { ((it - event.unlockedAt) / 1000).toString() }.orEmpty()
        val (kind, label) = when (val answer = event.answer) {
            Answer.Habit -> "habit" to ""
            Answer.AppCheck -> "app" to ""
            Answer.None -> "none" to ""
            Answer.Other -> "typed" to event.customText.orEmpty()
            is Answer.Picked -> "reason" to reasons[answer.reasonId]?.label.orEmpty()
        }
        out.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(time)).append(',')
            .append(seconds).append(',')
            .append(kind).append(',')
            .append(csvField(label)).append('\n')
    }
}

private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
