package cz.kutner.why.domain

import cz.kutner.why.data.db.Reason
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/**
 * Orders answers by how often they were given around this time of day, on the same kind of day.
 * With too few answers there, it widens to all days, then to all times.
 */
class TimeOfDayOrder(
    private val now: Long,
    private val zone: ZoneId,
    private val windowMinutes: Int = 90,
    private val minSample: Int = 10,
) {
    class Rank<K>(val forNow: Map<K, Int>, val overall: Map<K, Int>)

    fun <K> rank(uses: List<Pair<K, Long>>): Rank<K> {
        val nowAt = Instant.ofEpochMilli(now).atZone(zone)
        val nowMinute = nowAt.hour * 60 + nowAt.minute
        val nowWeekend = nowAt.dayOfWeek.isWeekend()
        val inWindow = uses.filter { (_, at) ->
            val z = Instant.ofEpochMilli(at).atZone(zone)
            circularDistance(z.hour * 60 + z.minute, nowMinute) <= windowMinutes
        }
        val sameDayType = inWindow.filter { (_, at) -> Instant.ofEpochMilli(at).atZone(zone).dayOfWeek.isWeekend() == nowWeekend }
        val chosen = when {
            sameDayType.size >= minSample -> sameDayType
            inWindow.size >= minSample -> inWindow
            else -> uses
        }
        return Rank(chosen.countByKey(), uses.countByKey())
    }

    fun order(reasons: List<Reason>, answers: List<Pair<Long, Long>>): List<Reason> {
        val rank = rank(answers)
        return reasons.sortedWith(
            compareByDescending<Reason> { rank.forNow[it.id] ?: 0 }
                .thenByDescending { rank.overall[it.id] ?: 0 }
                .thenBy { it.position },
        )
    }

    private fun <K> List<Pair<K, Long>>.countByKey(): Map<K, Int> = groupingBy { it.first }.eachCount()

    private fun circularDistance(a: Int, b: Int): Int {
        val d = abs(a - b)
        return min(d, MINUTES_PER_DAY - d)
    }

    private fun DayOfWeek.isWeekend() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
