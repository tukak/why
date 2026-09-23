package cz.kutner.why.domain

import cz.kutner.why.data.db.Reason
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
    private val rules = zone.rules

    class Rank<K>(val forNow: Map<K, Int>, val overall: Map<K, Int>)

    fun <K> rank(uses: List<Pair<K, Long>>): Rank<K> {
        val nowLocal = localMillis(now)
        val nowMinute = minuteOfDay(nowLocal)
        val nowWeekend = isWeekend(nowLocal)
        val sameDayType = HashMap<K, Int>()
        val inWindow = HashMap<K, Int>()
        val overall = HashMap<K, Int>()
        var sameDayTypeTotal = 0
        var inWindowTotal = 0
        for ((key, at) in uses) {
            overall.merge(key, 1, Int::plus)
            val local = localMillis(at)
            if (circularDistance(minuteOfDay(local), nowMinute) > windowMinutes) continue
            inWindow.merge(key, 1, Int::plus)
            inWindowTotal++
            if (isWeekend(local) == nowWeekend) {
                sameDayType.merge(key, 1, Int::plus)
                sameDayTypeTotal++
            }
        }
        val forNow = when {
            sameDayTypeTotal >= minSample -> sameDayType
            inWindowTotal >= minSample -> inWindow
            else -> overall
        }
        return Rank(forNow, overall)
    }

    fun order(reasons: List<Reason>, answers: List<Pair<Long, Long>>): List<Reason> {
        val rank = rank(answers)
        return reasons.sortedWith(
            compareByDescending<Reason> { rank.forNow[it.id] ?: 0 }
                .thenByDescending { rank.overall[it.id] ?: 0 }
                .thenBy { it.id },
        )
    }

    private fun circularDistance(a: Int, b: Int): Int {
        val d = abs(a - b)
        return min(d, MINUTES_PER_DAY - d)
    }

    private fun localMillis(at: Long): Long = at + rules.getOffset(Instant.ofEpochMilli(at)).totalSeconds * 1000L

    private fun minuteOfDay(local: Long): Int = Math.floorMod(Math.floorDiv(local, 60_000L), MINUTES_PER_DAY.toLong()).toInt()

    /** Day 0 of the epoch was a Thursday; +3 makes Monday 0, so Saturday and Sunday are 5 and 6. */
    private fun isWeekend(local: Long): Boolean = Math.floorMod(Math.floorDiv(local, 86_400_000L) + 3, 7L) >= 5

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
