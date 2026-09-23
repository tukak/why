package cz.kutner.why.ui.week

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.domain.Answer
import cz.kutner.why.domain.DayBar
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.domain.summarizeWeek
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.components.UiText
import cz.kutner.why.ui.formatAverage
import cz.kutner.why.ui.formatDuration
import cz.kutner.why.ui.minuteTicker
import cz.kutner.why.ui.styleOf
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import java.time.Clock
import java.time.format.TextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimeRow(val label: UiText, val style: PebbleStyle, val millis: Long, val fraction: Float)

data class WeekUiState(
    val days: List<DayBar>,
    val habitAvgMillis: Long?,
    val reasonAvgMillis: Long?,
    val time: List<TimeRow>,
)

class WeekViewModel(unlocks: UnlockRepository, private val clock: Clock) : ViewModel() {
    private val from = startOfDay(clock.millis(), clock.zone) - 6 * 24 * 60 * 60 * 1000L

    val state: StateFlow<WeekUiState?> = combine(unlocks.reasons, unlocks.eventsSince(from), minuteTicker()) { reasons, events, _ ->
        val summary = summarizeWeek(events, clock.millis(), clock.zone)
        val byId = reasons.associateBy { it.id }
        val rows = summary.timeByAnswer.filterKeys { it != Answer.None }.entries.sortedByDescending { it.value }.take(4)
        val max = rows.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
        WeekUiState(
            days = summary.days,
            habitAvgMillis = summary.habitAvgMillis,
            reasonAvgMillis = summary.reasonAvgMillis,
            time = rows.map { (answer, millis) ->
                val label = when (answer) {
                    Answer.Habit -> UiText.Res(R.string.habit)
                    is Answer.Picked -> byId[answer.reasonId]?.let { UiText.Raw(it.label) } ?: UiText.Res(R.string.answer_other)
                    else -> UiText.Res(R.string.answer_other)
                }
                TimeRow(label, styleOf(answer, byId), millis, millis / max.toFloat())
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun WeekScreen() {
    val app = LocalContext.current.container
    val vm = viewModel { WeekViewModel(app.unlocks, app.clock) }
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
        Text(stringResource(R.string.week_title), style = MaterialTheme.typography.headlineLarge)
        val current = state ?: return@Column
        InsightCard(current.habitAvgMillis, current.reasonAvgMillis)
        DailyJars(current.days)
        if (current.time.isNotEmpty()) TimeSection(current.time)
    }
}

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
private fun Stat(value: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun DailyJars(days: List<DayBar>) {
    val colors = MaterialTheme.colorScheme
    val max = days.maxOf { it.total }.coerceAtLeast(1)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.week_daily), style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Box(Modifier.fillMaxWidth().height(96.dp * ((day.total - day.habit) / max.toFloat())).background(colors.outlineVariant))
                        Box(Modifier.fillMaxWidth().height(96.dp * (day.habit / max.toFloat())).background(ReasonColor.Ember.tones.ink))
                    }
                    Text("${day.total}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
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
                Box(Modifier.size(32.dp).background(row.style.color.tones.container, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    Pebble(row.style, 18.dp)
                }
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

