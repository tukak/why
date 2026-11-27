package cz.kutner.why.ui.week

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.UnlockEvent
import cz.kutner.why.domain.Answer
import cz.kutner.why.domain.HabitHeatMap
import cz.kutner.why.domain.answer
import cz.kutner.why.domain.localDate
import cz.kutner.why.domain.startMillis
import cz.kutner.why.domain.weekChange
import cz.kutner.why.domain.weekStart
import cz.kutner.why.domain.weeklyTotals
import cz.kutner.why.domain.habitHeatMap
import cz.kutner.why.domain.summarizeWeek
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.components.ShapeBadge
import cz.kutner.why.ui.components.UiText
import cz.kutner.why.ui.formatAverage
import cz.kutner.why.ui.formatDuration
import cz.kutner.why.ui.labelOf
import cz.kutner.why.ui.minuteTicker
import cz.kutner.why.ui.styleOf
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import cz.kutner.why.ui.timeFormatter
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.sqrt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimeRow(val label: UiText, val style: PebbleStyle, val millis: Long, val fraction: Float)

/** A day's jar as color layers, bottom first. A day still to come has [future] set and no layers. */
data class DayJar(val date: LocalDate, val total: Int, val layers: List<Pair<ReasonColor, Int>>, val future: Boolean)

/** An earlier week in the small row of jars; heights are fractions of the tallest jar shown. */
data class MiniWeek(val start: LocalDate, val weeksBack: Int, val habit: Float, val rest: Float)

data class WeekUiState(
    val start: LocalDate,
    val weeksBack: Int,
    val oldestWeeksBack: Int,
    val days: List<DayJar>,
    /** Unlocks against the week before; null without a fair baseline. */
    val change: Int?,
    val habitAvgMillis: Long?,
    val reasonAvgMillis: Long?,
    val time: List<TimeRow>,
    val heatMap: HabitHeatMap,
    val earlier: List<MiniWeek>,
)

/** Color layers of a jar, bottom first: habit, then reason colors by count, grey (no clear reason) on top. */
fun jarLayers(events: List<UnlockEvent>, byId: Map<Long, Reason>): List<Pair<ReasonColor, Int>> =
    events.groupingBy { styleOf(it.answer, byId).color }.eachCount().entries.sortedWith(
        compareBy<Map.Entry<ReasonColor, Int>> {
            when (it.key) {
                ReasonColor.Ember -> 0
                ReasonColor.Stone -> 2
                else -> 1
            }
        }.thenByDescending { it.value },
    ).map { it.key to it.value }

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(unlocks: UnlockRepository, private val clock: Clock, private val firstDay: DayOfWeek, initial: LocalDate?) : ViewModel() {
    private fun thisWeek() = weekStart(LocalDate.now(clock), firstDay)

    private val weeksBack = MutableStateFlow(initial?.let { ChronoUnit.WEEKS.between(it, thisWeek()).toInt() } ?: 0)

    fun select(weeksBack: Int) {
        this.weeksBack.value = weeksBack
    }

    val state: StateFlow<WeekUiState?> = combine(weeksBack, unlocks.firstUnlock) { back, first -> back to first }
        .flatMapLatest { (back, first) ->
            val zone = clock.zone
            val current = thisWeek()
            val oldest = first?.let { ChronoUnit.WEEKS.between(weekStart(localDate(it, zone), firstDay), current).toInt() } ?: 0
            val selected = back.coerceIn(0, oldest)
            val start = current.minusWeeks(selected.toLong())
            // The row of small jars keeps the selected week in view, with up to 3 newer weeks beside it.
            val shown = minOf(MINI_WEEKS, oldest + 1)
            val newest = (selected - 3).coerceIn(0, oldest + 1 - shown)
            val loadFrom = minOf(current.minusWeeks((newest + shown - 1).toLong()), start.minusWeeks(HEAT_MAP_WEEKS - 1L))
            val loadTo = current.minusWeeks(newest.toLong()).plusWeeks(1)
            combine(unlocks.reasons, unlocks.eventsBetween(startMillis(loadFrom, zone), startMillis(loadTo, zone)), minuteTicker()) { reasons, events, _ ->
                val now = clock.millis()
                val today = LocalDate.now(clock)
                val weekFrom = startMillis(start, zone)
                val weekTo = startMillis(start.plusWeeks(1), zone)
                val summary = summarizeWeek(events.filter { it.unlockedAt in weekFrom until weekTo }, start, now, zone)
                val byId = reasons.associateBy { it.id }
                val rows = summary.timeByAnswer.filterKeys { it != Answer.None }.entries.sortedByDescending { it.value }.take(4)
                val max = rows.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
                val heatFrom = startMillis(start.minusWeeks(HEAT_MAP_WEEKS - 1L), zone)
                WeekUiState(
                    start = start,
                    weeksBack = selected,
                    oldestWeeksBack = oldest,
                    days = summary.days.map { DayJar(it.date, it.events.size, jarLayers(it.events, byId), future = it.date.isAfter(today)) },
                    change = first?.let { weekChange(events, start, it, now, zone) },
                    habitAvgMillis = summary.habitAvgMillis,
                    reasonAvgMillis = summary.reasonAvgMillis,
                    time = rows.map { (answer, millis) -> TimeRow(labelOf(answer, byId), styleOf(answer, byId), millis, millis / max.toFloat()) },
                    heatMap = habitHeatMap(events.filter { it.unlockedAt in heatFrom until weekTo }, zone),
                    earlier = miniWeeks(events, first, now, current, newest, shown),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun miniWeeks(events: List<UnlockEvent>, first: Long?, now: Long, current: LocalDate, newest: Int, shown: Int): List<MiniWeek> {
        val oldestShown = current.minusWeeks((newest + shown - 1).toLong())
        val newestShown = current.minusWeeks(newest.toLong())
        val weeks = weeklyTotals(events, first ?: now, now, clock.zone, firstDay)
            .filter { !it.start.isBefore(oldestShown) && !it.start.isAfter(newestShown) }
        val max = weeks.maxOfOrNull { it.perDay }?.takeIf { it > 0 } ?: 1.0
        return weeks.map {
            val height = (it.perDay / max).toFloat()
            MiniWeek(it.start, ChronoUnit.WEEKS.between(it.start, current).toInt(), height * it.habitShare.toFloat(), height * (1 - it.habitShare.toFloat()))
        }
    }

    private companion object {
        const val HEAT_MAP_WEEKS = 4
        const val MINI_WEEKS = 8
    }
}

/** A jar or card that is not filled yet. */
fun Modifier.dashedOutline(color: Color, corner: Dp): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(corner.toPx()),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

/** "Nov 16 – 22" in the phone's language. */
@Composable
fun weekRange(start: LocalDate, withYear: Boolean = false): String {
    val context = LocalContext.current
    fun noon(date: LocalDate) = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    val year = if (withYear) DateUtils.FORMAT_SHOW_YEAR else DateUtils.FORMAT_NO_YEAR
    return DateUtils.formatDateRange(context, noon(start), noon(start.plusDays(6)), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_UTC or year)
}

/** "Nov 16" in the phone's language. */
@Composable
fun monthDay(date: LocalDate): String {
    val locale = LocalLocale.current.platformLocale
    val format = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale) }
    return date.format(format)
}

@Composable
fun firstDayOfWeek(): DayOfWeek = WeekFields.of(LocalLocale.current.platformLocale).firstDayOfWeek

@Composable
fun BackButton(onBack: () -> Unit) {
    val label = stringResource(R.string.back)
    FilledTonalIconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = label }) {
        LineIcon(Icons.ArrowLeft, MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** The Week tab, or with [initial] a week opened from All weeks; [onBack] is set when it is not a tab. */
@Composable
fun WeekScreen(onAllWeeks: () -> Unit, initial: LocalDate? = null, onBack: (() -> Unit)? = null) {
    val app = LocalContext.current.container
    val firstDay = firstDayOfWeek()
    val vm = viewModel { WeekViewModel(app.unlocks, app.clock, firstDay, initial) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        val current = state
        if (current == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                onBack?.let { BackButton(it) }
                Text(stringResource(R.string.week_title), style = MaterialTheme.typography.headlineLarge)
            }
            return@Column
        }
        WeekHeader(current, onBack, onSelect = vm::select)
        InsightCard(current.habitAvgMillis, current.reasonAvgMillis)
        DailyJars(current.days, current.change, thisWeek = current.weeksBack == 0)
        if (current.time.isNotEmpty()) TimeSection(current.time)
        if (current.heatMap.total >= HEAT_MAP_MIN_HABITS) HabitHours(current.heatMap, until = current.start.plusDays(6).takeIf { current.weeksBack > 0 })
        EarlierWeeks(current.earlier, selected = current.weeksBack, onSelect = vm::select, onAllWeeks = onAllWeeks)
    }
}

@Composable
private fun WeekHeader(state: WeekUiState, onBack: (() -> Unit)?, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val back = state.weeksBack
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            onBack?.let { BackButton(it) }
            Column(Modifier.weight(1f)) {
                Text(
                    when (back) {
                        0 -> stringResource(R.string.week_title)
                        1 -> stringResource(R.string.week_last)
                        else -> weekRange(state.start)
                    },
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    when (back) {
                        0 -> stringResource(R.string.week_so_far, weekRange(state.start))
                        1 -> weekRange(state.start)
                        else -> state.start.year.toString()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
            }
            StepButton(Icons.ChevronLeft, stringResource(R.string.week_previous), enabled = back < state.oldestWeeksBack) { onSelect(back + 1) }
            StepButton(Icons.ChevronRight, stringResource(R.string.week_next), enabled = back > 0) { onSelect(back - 1) }
        }
        if (back > 1) {
            Button(onClick = { onSelect(0) }, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Text(stringResource(R.string.week_back_to_now))
            }
        }
    }
}

@Composable
private fun StepButton(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.semantics { contentDescription = label }) {
        LineIcon(icon, if (enabled) colors.onSecondaryContainer else colors.onSurface.copy(alpha = 0.38f))
    }
}

@Composable
private fun EarlierWeeks(weeks: List<MiniWeek>, selected: Int, onSelect: (Int) -> Unit, onAllWeeks: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ember = ReasonColor.Ember.tones
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.week_earlier), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onAllWeeks, contentPadding = PaddingValues(start = 14.dp, end = 8.dp)) {
                Text(stringResource(R.string.week_all_weeks))
                LineIcon(Icons.ChevronRight, colors.onSecondaryContainer, size = 18.dp)
            }
        }
        if (weeks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                weeks.forEach { week ->
                    val isSelected = week.weeksBack == selected
                    val label = if (week.weeksBack == 0) stringResource(R.string.week_now) else monthDay(week.start)
                    val description = stringResource(R.string.week_of, monthDay(week.start))
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ember.container else Color.Transparent)
                            .selectable(isSelected, role = Role.Button) { onSelect(week.weeksBack) }
                            .semantics { contentDescription = description }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Column(
                            Modifier.width(26.dp).height(MINI_JAR).clip(RoundedCornerShape(9.dp)).background(colors.surfaceContainerHigh),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(Modifier.fillMaxWidth().height(MINI_JAR * week.rest).background(ReasonColor.Blue.tones.ink.copy(alpha = 0.6f)))
                            Box(Modifier.fillMaxWidth().height(MINI_JAR * week.habit).background(ember.ink))
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(stringResource(R.string.week_earlier_caption), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

private val MINI_JAR = 56.dp

/** Fewer habit unlocks than this make a noisy grid, not a pattern. */
private const val HEAT_MAP_MIN_HABITS = 5

/** [until] is the last day of an earlier week; null means the 4 weeks up to now. */
@Composable
private fun HabitHours(map: HabitHeatMap, until: LocalDate?) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val hourFormat = remember(locale) { timeFormatter(context, locale, hourOnly = true) }
    fun hour(h: Int) = LocalTime.of(h % 24, 0).format(hourFormat)
    val days = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        (0L until 7).map { first.plus(it) }
    }
    val peak = map.peakHour?.let { stringResource(R.string.week_habit_peak, hour(it), hour(it + 1)) }
    val ink = ReasonColor.Ember.tones.ink
    val empty = colors.surfaceContainerHigh
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(stringResource(R.string.week_habit_hours), style = MaterialTheme.typography.titleSmall)
            val period = until?.let { stringResource(R.string.week_habit_hours_until, monthDay(it)) } ?: stringResource(R.string.week_habit_hours_period)
            Text(period, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(HEAT_GAP)) {
                days.forEach { day ->
                    Box(Modifier.height(HEAT_CELL), contentAlignment = Alignment.CenterStart) {
                        Text(day.getDisplayName(TextStyle.SHORT, locale), style = labelStyle, color = colors.onSurfaceVariant)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(HEAT_CELL * 7 + HEAT_GAP * 6)
                        .semantics { contentDescription = peak.orEmpty() },
                ) {
                    val gap = HEAT_GAP.toPx()
                    val cellW = (size.width - gap * 23) / 24
                    val cellH = HEAT_CELL.toPx()
                    val corner = CornerRadius(3.dp.toPx())
                    days.forEachIndexed { row, day ->
                        for (h in 0 until 24) {
                            val count = map.count(day, h)
                            // Square root keeps rare hours visible next to one very busy hour.
                            val strength = if (count == 0) 0f else 0.2f + 0.8f * sqrt(count / map.max.toFloat())
                            drawRoundRect(
                                color = if (count == 0) empty else ink.copy(alpha = strength),
                                topLeft = Offset(h * (cellW + gap), row * (cellH + gap)),
                                size = Size(cellW, cellH),
                                cornerRadius = corner,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf(0, 6, 12, 18).forEach { h ->
                        Text(hour(h), style = labelStyle, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (peak != null) Text(peak, style = MaterialTheme.typography.labelLarge)
    }
}

private val HEAT_CELL = 14.dp
private val HEAT_GAP = 2.dp

@Composable
private fun InsightCard(habitAvg: Long?, reasonAvg: Long?) {
    val tones = ReasonColor.Ember.tones
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(tones.container)
            .padding(22.dp),
    ) {
        Box(Modifier.matchParentSize()) {
            Pebble(PebbleStyle.Habit, 190.dp, Modifier.align(Alignment.TopEnd).offset(x = 100.dp, y = (-96).dp), color = tones.ink.copy(alpha = 0.16f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val ratio = if (habitAvg != null && reasonAvg != null && reasonAvg > 0) habitAvg / reasonAvg.toDouble() else null
            val headline = when {
                ratio == null -> stringResource(R.string.week_insight_empty)
                ratio >= 1.2 -> stringResource(R.string.week_insight_longer, ratio)
                ratio <= 0.8 -> stringResource(R.string.week_insight_shorter)
                else -> stringResource(R.string.week_insight_balanced)
            }
            Text(headline, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 60.dp))
            if (habitAvg != null || reasonAvg != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    habitAvg?.let { Stat(formatAverage(it), stringResource(R.string.habit_lower), tones.ink) }
                    reasonAvg?.let { Stat(formatAverage(it), stringResource(R.string.week_with_reason), colors.onSurface) }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun DailyJars(days: List<DayJar>, change: Int?, thisWeek: Boolean) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalLocale.current.platformLocale
    val resources = LocalResources.current
    val max = days.maxOf { it.total }.coerceAtLeast(1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.week_daily), style = MaterialTheme.typography.titleSmall)
            if (change != null) {
                val text = when {
                    change < 0 -> resources.getQuantityString(if (thisWeek) R.plurals.week_fewer_now else R.plurals.week_fewer_before, -change, -change)
                    change > 0 -> resources.getQuantityString(if (thisWeek) R.plurals.week_more_now else R.plurals.week_more_before, change, change)
                    else -> stringResource(if (thisWeek) R.string.week_same_now else R.string.week_same_before)
                }
                Text(text, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (day.future) {
                        Box(Modifier.width(38.dp).height(96.dp).dashedOutline(colors.outlineVariant, 14.dp))
                    } else {
                        Column(
                            Modifier
                                .width(38.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceContainer)
                                .graphicsLayer {
                                    scaleY = grow.value
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                },
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            day.layers.asReversed().forEach { (color, count) ->
                                Box(Modifier.fillMaxWidth().height(96.dp * (count / max.toFloat())).background(color.tones.ink))
                            }
                        }
                    }
                    Text(if (day.future) "–" else "${day.total}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} ${day.date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSection(rows: List<TimeRow>) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.week_time), style = MaterialTheme.typography.titleSmall)
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShapeBadge(row.style, 32.dp, 11.dp, 18.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(row.label.resolve(), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        Text(formatDuration(row.millis), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.surfaceContainerHigh)) {
                        Box(Modifier.fillMaxWidth(row.fraction.coerceIn(0.02f, 1f)).height(6.dp).background(row.style.color.tones.ink))
                    }
                }
            }
        }
    }
}

