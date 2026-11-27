package cz.kutner.why.ui.week

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.domain.HistoryTrends
import cz.kutner.why.domain.Trend
import cz.kutner.why.domain.WeekTotals
import cz.kutner.why.domain.localDate
import cz.kutner.why.domain.trends
import cz.kutner.why.domain.weekStart
import cz.kutner.why.domain.weeklyTotals
import cz.kutner.why.ui.formatDuration
import cz.kutner.why.ui.minuteTicker
import cz.kutner.why.ui.theme.ReasonColor
import java.text.NumberFormat
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HistoryRange(val weeks: Int?, val label: Int) {
    ThreeMonths(13, R.string.history_range_3m),
    Year(52, R.string.history_range_1y),
    All(null, R.string.history_range_all),
}

/** A week on the shelf; layer heights are fractions of the tallest jar. */
data class ShelfJar(val start: LocalDate, val layers: List<Pair<ReasonColor, Float>>)

data class ShelfRow(val month: YearMonth, val jars: List<ShelfJar>)

data class AllWeeksUiState(
    val range: HistoryRange,
    val firstDate: LocalDate?,
    /** Weeks in the range, oldest first. */
    val weeks: List<WeekTotals>,
    /** Whether the range starts at the first week, so "since you started" is true. */
    val fromStart: Boolean,
    /** Null until there are 2 weeks: one week is no trend. */
    val trends: HistoryTrends?,
    /** Newest month first; weeks inside a month oldest first. */
    val shelf: List<ShelfRow>,
)

class AllWeeksViewModel(unlocks: UnlockRepository, private val clock: Clock, private val firstDay: DayOfWeek) : ViewModel() {
    private val range = MutableStateFlow(HistoryRange.All)

    fun select(range: HistoryRange) {
        this.range.value = range
    }

    val state: StateFlow<AllWeeksUiState?> = combine(range, unlocks.firstUnlock, unlocks.eventsSince(0), unlocks.reasons, minuteTicker()) { range, first, events, reasons, _ ->
        val now = clock.millis()
        val zone = clock.zone
        val all = first?.let { weeklyTotals(events, it, now, zone, firstDay) }.orEmpty()
        val weeks = range.weeks?.let { all.takeLast(it) } ?: all
        val byId = reasons.associateBy { it.id }
        val byWeek = events.groupBy { weekStart(localDate(it.unlockedAt, zone), firstDay) }
        val max = weeks.maxOfOrNull { it.perDay }?.takeIf { it > 0 } ?: 1.0
        val shelf = weeks.groupBy { YearMonth.from(it.start) }.entries.reversed().map { (month, inMonth) ->
            ShelfRow(
                month,
                inMonth.map { week ->
                    val height = (week.perDay / max).toFloat()
                    val total = week.unlocks.coerceAtLeast(1).toFloat()
                    ShelfJar(week.start, jarLayers(byWeek[week.start].orEmpty(), byId).map { (color, count) -> color to height * count / total })
                },
            )
        }
        AllWeeksUiState(
            range = range,
            firstDate = first?.let { localDate(it, zone) },
            weeks = weeks,
            fromStart = weeks.size == all.size,
            trends = if (all.size >= 2 && weeks.size >= 2) trends(weeks.first(), weeks.last()) else null,
            shelf = shelf,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun AllWeeksScreen(onBack: () -> Unit, onWeek: (LocalDate) -> Unit) {
    val app = LocalContext.current.container
    val firstDay = firstDayOfWeek()
    val vm = viewModel { AllWeeksViewModel(app.unlocks, app.clock, firstDay) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Text(stringResource(R.string.week_all_weeks), style = MaterialTheme.typography.headlineMedium)
        }
        val current = state ?: return@Column
        val trends = current.trends
        if (trends == null) {
            FirstWeekCard(current.firstDate, firstDay)
            Shelf(current.shelf, onWeek, nextWeek = current.weeks.lastOrNull()?.start?.plusWeeks(1))
            ComingSoon()
        } else {
            RangePicker(current.range, onSelect = vm::select)
            SummaryCard(current, trends)
            Shelf(current.shelf, onWeek, nextWeek = null)
            val weeks = current.weeks
            val firstLabel = monthDay(weeks.first().start)
            BarChart(stringResource(R.string.history_chart_unlocks), null, weeks.map { it.perDay.toFloat() }, null, colors.onSurface, colors.outlineVariant, firstLabel)
            BarChart(
                stringResource(R.string.history_chart_habit),
                stringResource(R.string.history_chart_habit_hint),
                weeks.map { it.habitShare.toFloat() },
                scale = 1f,
                highlight = ReasonColor.Ember.tones.ink,
                muted = ReasonColor.Ember.tones.ink.copy(alpha = 0.45f),
                firstLabel = firstLabel,
                midLine = true,
            )
            BarChart(
                stringResource(R.string.history_chart_screen),
                stringResource(R.string.history_chart_screen_hint),
                weeks.map { it.screenPerDay.toFloat() },
                null,
                ReasonColor.Blue.tones.ink,
                ReasonColor.Blue.tones.ink.copy(alpha = 0.35f),
                firstLabel,
            )
        }
    }
}

@Composable
private fun RangePicker(selected: HistoryRange, onSelect: (HistoryRange) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryRange.entries.forEach { range ->
            val isSelected = range == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (isSelected) colors.primary else colors.surfaceContainerHigh, RoundedCornerShape(22.dp))
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(range) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(range.label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: AllWeeksUiState, trends: HistoryTrends) {
    val tones = ReasonColor.Ember.tones
    val colors = MaterialTheme.colorScheme
    val locale = LocalLocale.current.platformLocale
    val percent = remember(locale) { NumberFormat.getPercentInstance(locale) }
    val first = state.weeks.first()
    val last = state.weeks.last()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(tones.container).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val since = when {
            state.fromStart && state.firstDate != null -> stringResource(R.string.history_since, startDate(state.firstDate))
            state.range == HistoryRange.ThreeMonths -> stringResource(R.string.history_last_3m)
            else -> stringResource(R.string.history_last_1y)
        }
        Text(since, style = MaterialTheme.typography.labelLarge, color = colors.onTertiaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FromTo("${first.perDay.roundToInt()} → ${last.perDay.roundToInt()}", stringResource(R.string.history_unlocks_per_day), colors.onSurface, Modifier.weight(1f))
            FromTo("${percent.format(first.habitShare)} → ${percent.format(last.habitShare)}", stringResource(R.string.habit_lower), tones.ink, Modifier.weight(1f))
        }
        FromTo(
            "${formatDuration(first.screenPerDay.roundToLong())} → ${formatDuration(last.screenPerDay.roundToLong())}",
            stringResource(R.string.history_screen_per_day),
            ReasonColor.Blue.tones.ink,
        )
        Text(headline(trends), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FromTo(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/** "Fewer unlocks and less habit, but more screen time." */
@Composable
private fun headline(trends: HistoryTrends): String {
    val parts = listOf(
        trends.unlocks to (R.string.history_fewer_unlocks to R.string.history_more_unlocks),
        trends.habit to (R.string.history_less_habit to R.string.history_more_habit),
        trends.screen to (R.string.history_less_screen to R.string.history_more_screen),
    )
    val better = parts.filter { it.first == Trend.Better }.map { stringResource(it.second.first) }
    val worse = parts.filter { it.first == Trend.Worse }.map { stringResource(it.second.second) }
    @Composable
    fun join(items: List<String>): String = when (items.size) {
        1 -> items[0]
        2 -> stringResource(R.string.history_list_two, items[0], items[1])
        else -> stringResource(R.string.history_list_three, items[0], items[1], items[2])
    }
    val text = when {
        better.isEmpty() && worse.isEmpty() -> return stringResource(R.string.history_same)
        better.isNotEmpty() && worse.isNotEmpty() -> stringResource(R.string.history_but, join(better), join(worse))
        else -> join(better.ifEmpty { worse })
    }
    val locale = LocalLocale.current.platformLocale
    return text.replaceFirstChar { it.titlecase(locale) } + "."
}

@Composable
private fun startDate(date: LocalDate): String {
    val locale = LocalLocale.current.platformLocale
    val skeleton = if (date.year == LocalDate.now().year) "MMMd" else "yMMMd"
    val format = remember(locale, skeleton) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale) }
    return date.format(format)
}

@Composable
private fun FirstWeekCard(firstDate: LocalDate?, firstDay: DayOfWeek) {
    val tones = ReasonColor.Ember.tones
    val colors = MaterialTheme.colorScheme
    val progress = firstDate?.let {
        val daysIn = ChronoUnit.DAYS.between(weekStart(it, firstDay), LocalDate.now()) + 1
        (daysIn / 7f).coerceIn(0f, 1f)
    } ?: 0f
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(tones.container).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.history_first_week), style = MaterialTheme.typography.labelLarge, color = colors.onTertiaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(tones.ink.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).background(tones.ink))
            }
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(tones.ink.copy(alpha = 0.25f)))
        }
        Text(stringResource(R.string.history_first_title), style = MaterialTheme.typography.titleLarge)
        if (firstDate != null) {
            Text(stringResource(R.string.history_first_started, startDate(firstDate)), style = MaterialTheme.typography.bodyMedium, color = colors.onTertiaryContainer)
        }
    }
}

@Composable
private fun ComingSoon() {
    val colors = MaterialTheme.colorScheme
    val border = colors.outlineVariant
    Column(
        Modifier
            .fillMaxWidth()
            .dashedOutline(border, 24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.history_coming), style = MaterialTheme.typography.titleSmall)
        listOf(R.string.history_chart_unlocks, R.string.history_chart_habit, R.string.history_chart_screen).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Shelf(rows: List<ShelfRow>, onWeek: (LocalDate) -> Unit, nextWeek: LocalDate?) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalLocale.current.platformLocale
    val thisYear = LocalDate.now().year
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(stringResource(R.string.history_shelf), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.history_shelf_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (rows.isEmpty()) return@Column
        Column(
            Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
        ) {
            rows.forEachIndexed { index, row ->
                val skeleton = if (row.month.year == thisYear) "MMM" else "MMMyy"
                val format = remember(locale, skeleton) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale) }
                val shelfColor = colors.outlineVariant
                Row(
                    Modifier
                        .fillMaxWidth()
                        .drawBehindShelf(shelfColor)
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        row.month.format(format),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.widthIn(min = 34.dp).padding(bottom = 20.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        row.jars.forEach { jar -> ShelfJarView(jar, onWeek) }
                        if (index == 0 && nextWeek != null) EmptyJar(nextWeek)
                    }
                }
                Box(Modifier.height(10.dp))
            }
        }
    }
}

private fun Modifier.drawBehindShelf(color: Color): Modifier = drawBehind {
    val width = 3.dp.toPx()
    drawLine(color, Offset(0f, size.height - width / 2), Offset(size.width, size.height - width / 2), width)
}

private val SHELF_JAR = 72.dp

@Composable
private fun ShelfJarView(jar: ShelfJar, onWeek: (LocalDate) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.week_of, monthDay(jar.start))
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { onWeek(jar.start) }
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(
            Modifier
                .width(40.dp)
                .height(SHELF_JAR)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(colors.surfaceContainer),
            verticalArrangement = Arrangement.Bottom,
        ) {
            jar.layers.asReversed().forEach { (color, fraction) ->
                Box(Modifier.fillMaxWidth().height(SHELF_JAR * fraction).background(color.tones.ink))
            }
        }
        Text("${jar.start.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun EmptyJar(start: LocalDate) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.width(40.dp).height(SHELF_JAR).dashedOutline(colors.outlineVariant, 14.dp))
        Text("${start.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun BarChart(
    title: String,
    hint: String?,
    values: List<Float>,
    scale: Float?,
    highlight: Color,
    muted: Color,
    firstLabel: String,
    midLine: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val max = scale ?: values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val gap = if (values.size > 26) 1.dp else 4.dp
    val line = colors.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
        Box(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            Canvas(Modifier.matchParentSize()) {
                val gapPx = gap.toPx()
                val barWidth = (size.width - gapPx * (values.size - 1)) / values.size
                values.forEachIndexed { i, value ->
                    val height = size.height * (value / max).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = if (i == values.lastIndex) highlight else muted,
                        topLeft = Offset(i * (barWidth + gapPx), size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(minOf(barWidth / 2, 5.dp.toPx())),
                    )
                }
                if (midLine) {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = dash)
                    drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx(), pathEffect = dash)
                }
            }
            if (midLine) {
                val locale = LocalLocale.current.platformLocale
                Text(
                    remember(locale) { NumberFormat.getPercentInstance(locale).format(0.5) },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 14.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(firstLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.history_this_week), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

private val CHART_HEIGHT = 96.dp
