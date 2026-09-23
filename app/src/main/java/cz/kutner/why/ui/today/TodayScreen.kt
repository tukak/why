package cz.kutner.why.ui.today

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.formatAverage
import cz.kutner.why.ui.formatDuration
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

@Composable
fun TodayScreen(onSettings: () -> Unit) {
    val app = LocalContext.current.container
    val vm = viewModel { TodayViewModel(app.unlocks, app.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    state?.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(stringResource(R.string.today_title), style = MaterialTheme.typography.headlineLarge)
            }
            val settingsLabel = stringResource(R.string.settings_title)
            FilledTonalIconButton(onClick = onSettings, modifier = Modifier.size(48.dp).semantics { contentDescription = settingsLabel }) {
                LineIcon(Icons.Gear, colors.onSecondaryContainer)
            }
        }

        val current = state ?: return@Column
        PebbleJar(current)

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = colors.onSurface, fontWeight = FontWeight.Bold)) { append(formatDuration(current.screenMillis)) }
                append(" ")
                append(stringResource(R.string.today_on_screen))
                append(" · ")
                withStyle(SpanStyle(color = colors.onSurface, fontWeight = FontWeight.Bold)) { append(formatAverage(current.avgMillis)) }
                append(" ")
                append(stringResource(R.string.today_per_unlock))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        Legend(current.legend)
    }
}

@Composable
private fun PebbleJar(state: TodayUiState) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(JAR_HEIGHT)
            .clip(RoundedCornerShape(JAR_CORNER))
            .background(colors.surfaceContainer),
    ) {
        val layout = remember(state.pebbles.size, maxWidth) { jarLayout(state.pebbles.size, maxWidth, maxHeight) }
        if (animationsOff()) StaticPile(state.pebbles, layout) else FallingPile(state.pebbles, layout)

        // The badge sits top right; only when a large font leaves no room does it wrap below the number.
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.padding(end = 12.dp)) {
                Text("${state.unlocks}", style = MaterialTheme.typography.displayLarge, maxLines = 1, softWrap = false)
                Text(
                    pluralStringResource(R.plurals.today_unlocks, state.unlocks),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            state.fewerThanUsual?.takeIf { it != 0 }?.let { UsualBadge(it) }
        }
    }
}

/** Pebbles that fall in, roll with the phone's tilt and bounce when it is shaken. */
@Composable
private fun BoxWithConstraintsScope.FallingPile(pebbles: List<PebbleStyle>, layout: JarLayout) {
    val density = LocalDensity.current
    val wakes = remember { MutableStateFlow(0) }
    val sizePx = with(density) { layout.size.toPx() }
    val world = remember {
        with(density) { JarWorld(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), JAR_CORNER.toPx(), 4.dp.toPx()) }
    }
    LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
        world.resize(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        wakes.value++
    }
    val tilt = rememberTiltSensor { wakes.value++ }
    val pxPerMeter = with(density) { PX_PER_METER.toPx() }

    LaunchedEffect(world, pebbles.size, sizePx) {
        val fillFromLayout = world.bodies.isEmpty()
        world.setSize(sizePx)
        while (world.bodies.size > pebbles.size) world.removeLast()
        while (world.bodies.size < pebbles.size) {
            val i = world.bodies.size
            val shape = pebbles[i].shape
            if (fillFromLayout) {
                val slot = layout.slot(i)
                with(density) {
                    world.add(slot.x.toPx() + sizePx / 2, slot.y.toPx() + sizePx / 2, sizePx, Math.toRadians(slot.rotation.toDouble()).toFloat(), shape)
                }
            } else {
                val jitter = ((i * 37) % 21 - 10) / 10f * sizePx
                world.add(constraints.maxWidth / 2f + jitter, sizePx, sizePx, 0f, shape)
            }
        }
        world.wake()
        wakes.value++
    }

    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(world) {
        var seenWake = wakes.value
        var last = 0L
        while (true) {
            if (world.resting) {
                seenWake = wakes.first { it != seenWake }
                world.wake()
                last = 0L
            } else if (wakes.value != seenWake) {
                seenWake = wakes.value
                world.wake()
            }
            withFrameNanos { now ->
                val dt = if (last == 0L) 1 / 60f else ((now - last) / 1e9f).coerceAtMost(1 / 30f)
                last = now
                world.step(dt, tilt.ax * pxPerMeter, tilt.ay * pxPerMeter)
                frame = now
            }
        }
    }

    // Stamping a ready bitmap is much cheaper than filling hundreds of vector paths every frame.
    val inks = ReasonColor.entries.associateWith { it.tones.ink }
    val styles = pebbles.toSet()
    val stamps = remember(sizePx, inks, styles) { PebbleStamps(sizePx.toInt().coerceAtLeast(1), inks, density, styles) }
    Canvas(Modifier.matchParentSize()) {
        frame
        world.bodies.forEachIndexed { i, body ->
            val style = pebbles.getOrNull(i) ?: return@forEachIndexed
            rotate(Math.toDegrees(body.angle.toDouble()).toFloat(), pivot = Offset(body.x, body.y)) {
                drawImage(stamps.of(style), Offset(body.x - stamps.size / 2f, body.y - stamps.size / 2f))
            }
        }
    }
}

/** One bitmap per style, drawn and uploaded before the first frame that uses it. */
private class PebbleStamps(val size: Int, private val inks: Map<ReasonColor, Color>, private val density: Density, styles: Set<PebbleStyle>) {
    private val cache = styles.associateWith { render(it) }

    fun of(style: PebbleStyle): ImageBitmap = cache[style] ?: render(style)

    private fun render(style: PebbleStyle): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val area = Size(size.toFloat(), size.toFloat())
        val outline = style.shape.shape.createOutline(area, LayoutDirection.Ltr, density)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), area) {
            drawOutline(outline, inks.getValue(style.color))
        }
        bitmap.prepareToDraw()
        return bitmap
    }
}

@Composable
private fun StaticPile(pebbles: List<PebbleStyle>, layout: JarLayout) {
    pebbles.forEachIndexed { i, style ->
        val slot = layout.slot(i)
        Pebble(style, slot.size, Modifier.offset(slot.x, slot.y).rotate(slot.rotation))
    }
}

@Composable
private fun animationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

@Composable
private fun UsualBadge(fewer: Int) {
    val good = fewer > 0
    val tones = if (good) ReasonColor.Green.tones else ReasonColor.Ember.tones
    Row(
        Modifier.background(tones.container, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LineIcon(if (good) Icons.ArrowDown else Icons.ArrowUp, tones.ink, size = 14.dp, strokeWidth = 3f)
        Text(
            pluralStringResource(if (good) R.plurals.today_fewer else R.plurals.today_more, abs(fewer), abs(fewer)),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tones.ink,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(items: List<LegendItem>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) items else items.take(LEGEND_LIMIT)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { item ->
            Row(
                Modifier
                    .height(36.dp)
                    .background(item.style.color.tones.container, RoundedCornerShape(18.dp))
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Pebble(item.style, 18.dp)
                Text(item.label.resolve(), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text("${item.count}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        }
        if (items.size > LEGEND_LIMIT) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.height(36.dp)) {
                Text(
                    if (expanded) stringResource(R.string.today_legend_less) else stringResource(R.string.today_legend_more, items.size - LEGEND_LIMIT),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private const val LEGEND_LIMIT = 6

private data class Slot(val x: Dp, val y: Dp, val size: Dp, val rotation: Float)

private class JarLayout(val size: Dp, private val perRow: Int, private val width: Dp, private val height: Dp) {
    fun slot(i: Int): Slot {
        val row = i / perRow
        val col = i % perRow
        val step = size * 0.95f
        val brick = if (row % 2 == 1) size / 2 else 0.dp
        val jitterX = ((i * 13) % 5 - 2).dp
        val jitterY = ((i * 11) % 5 - 2).dp
        val x = (12.dp + brick + step * col + jitterX).coerceAtMost(width - size - 6.dp)
        val y = height - 12.dp - size - size * 0.8f * row + jitterY
        return Slot(x, y, size, rotation = ((i * 47) % 70 - 35).toFloat())
    }
}

private val JAR_HEIGHT = 330.dp
private val MIN_PEBBLE = 10.dp
private val JAR_CORNER = 40.dp

/** How far one m/s² moves a pebble; picked so a pebble falls through the jar in about 0.4 s. */
private val PX_PER_METER = 420.dp


/** Shrinks pebbles as the day fills up so the pile stays in the lower part of the jar. */
private fun jarLayout(count: Int, width: Dp, height: Dp): JarLayout {
    var size = 40.dp
    while (true) {
        val perRow = floor(((width - 24.dp - size / 2) / (size * 0.95f))).toInt().coerceAtLeast(1)
        val rows = ceil(count / perRow.toFloat())
        if (size <= MIN_PEBBLE || size * 0.8f * rows + size * 0.2f <= height * 0.6f) return JarLayout(size, perRow, width, height)
        size -= 2.dp
    }
}
