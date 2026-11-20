package cz.kutner.why.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import cz.kutner.why.R
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.Bricolage
import cz.kutner.why.ui.theme.Figtree
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import cz.kutner.why.ui.today.rememberTiltSensor
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What every layout needs from the question screen. */
internal class AnswerUi(
    val awake: (Choice) -> Boolean,
    val tap: (Choice) -> Unit,
    val label: (Choice) -> String,
    val hold: Boolean,
)

/** The usual two-column grid, also used for three columns and a one-column list. [wide] goes below the grid. */
@Composable
internal fun GridAnswers(items: List<Choice>, wide: Choice?, columns: Int, list: Boolean, ui: AnswerUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        val grid = rememberScrollState()
        Box(Modifier.weight(1f, fill = false)) {
            Column(Modifier.verticalScroll(grid), verticalArrangement = Arrangement.spacedBy(if (list) 8.dp else 10.dp)) {
                items.chunked(columns).forEachIndexed { row, group ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.forEachIndexed { col, choice ->
                            GridChip(choice, row * columns + col, columns, list, ui, Modifier.weight(1f))
                        }
                        repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (grid.canScrollForward) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.background))),
                )
            }
        }
        if (wide != null) {
            Spacer(Modifier.height(16.dp))
            WideAnswer(wide, ui)
        }
    }
}

@Composable
private fun GridChip(choice: Choice, index: Int, columns: Int, list: Boolean, ui: AnswerUi, modifier: Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 15L)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val radius = when {
        list -> 23.dp
        columns >= 3 -> 24.dp
        else -> 28.dp
    }
    AnswerSurface(
        shape = RoundedCornerShape(radius),
        color = choice.style.color.tones.container,
        awake = ui.awake(choice),
        hold = ui.hold,
        onTap = { ui.tap(choice) },
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .heightIn(min = if (list) 52.dp else 92.dp)
            .fillMaxHeight()
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationY = (1f - appear.value) * 40f
                scaleX = 0.94f + 0.06f * appear.value
                scaleY = scaleX
            },
    ) {
        if (list) {
            Row(Modifier.fillMaxHeight().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pebble(choice.style, 22.dp)
                Text(ui.label(choice), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else {
            val compact = columns >= 3
            Column(
                Modifier.fillMaxHeight().padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 12.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Pebble(choice.style, if (compact) 24.dp else 30.dp)
                Text(
                    ui.label(choice),
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    maxLines = if (compact) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The full-width button below the grid: "Just habit", or the reason the habit trap moved there. */
@Composable
private fun WideAnswer(choice: Choice, ui: AnswerUi) {
    val habit = choice.reason == null
    // One turn on appear; a never-ending spin would redraw the window all the time.
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (habit) angle.animateTo(360f, tween(1_400, easing = FastOutSlowInEasing)) }
    AnswerSurface(
        shape = RoundedCornerShape(28.dp),
        color = choice.style.color.tones.container,
        awake = ui.awake(choice),
        hold = ui.hold,
        onTap = { ui.tap(choice) },
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Pebble(choice.style, 40.dp, Modifier.graphicsLayer { rotationZ = angle.value })
            Column {
                Text(ui.label(choice), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (habit) {
                    Text(stringResource(R.string.habit_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

/** A small fixed-size answer: pebble above a label that shrinks to fit. */
@Composable
private fun CompactAnswer(
    choice: Choice,
    ui: AnswerUi,
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tappable: Boolean = true,
) {
    AnswerSurface(
        shape = shape,
        color = choice.style.color.tones.container,
        awake = ui.awake(choice),
        hold = ui.hold,
        tappable = tappable,
        onTap = { ui.tap(choice) },
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Pebble(choice.style, 18.dp)
            FitText(ui.label(choice), MaterialTheme.typography.labelLarge, 15.sp, Modifier.weight(1f, fill = false))
        }
    }
}

/** "Just habit" in the middle, the reasons in a ring around it. Used for up to [cz.kutner.why.domain.TrickSetup.CIRCLE_MAX_ANSWERS] answers. */
@Composable
internal fun CircleAnswers(items: List<Choice>, ui: AnswerUi, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Tiles at 30° steps clear their neighbours vertically only if a tile is at most a fifth of the height.
        val h = min(76.dp, maxHeight * 0.2f - 5.dp).coerceAtLeast(34.dp)
        val w = min(132.dp, maxWidth * 0.32f)
        val rx = maxWidth / 2 - w / 2 - 4.dp
        val ry = maxHeight / 2 - h / 2 - 4.dp
        val center = items.last()
        val ring = items.dropLast(1)
        val start = if (ring.size == 6) 30.0 else -90.0
        ring.forEachIndexed { k, choice ->
            val a = Math.toRadians(start + k * 360.0 / ring.size)
            val x = maxWidth / 2 + rx * cos(a).toFloat() - w / 2
            val y = maxHeight / 2 + ry * sin(a).toFloat() - h / 2
            CompactAnswer(choice, ui, Modifier.offset(x, y).size(w, h))
        }
        CompactAnswer(center, ui, Modifier.offset(maxWidth / 2 - w / 2, maxHeight / 2 - h / 2).size(w, h), CircleShape)
    }
}

/** Answers floating slowly around their place in a two-column grid; they never cross each other. */
@Composable
internal fun DriftAnswers(items: List<Choice>, seed: Long, ui: AnswerUi, modifier: Modifier = Modifier) {
    val speeds = remember(seed, items.size) { Random(seed).let { r -> items.map { r.nextInt(2_600, 5_200) to r.nextFloat() * 2f * PI.toFloat() } } }
    val transition = rememberInfiniteTransition(label = "drift")
    BoxWithConstraints(modifier.fillMaxSize()) {
        val rows = ceil(items.size / 2f).toInt().coerceAtLeast(1)
        val cellW = maxWidth / 2
        val cellH = maxHeight / rows
        val swing = ((cellH - 48.dp) / 2).coerceIn(2.dp, 14.dp)
        val w = cellW - swing * 2 - 6.dp
        val h = min(cellH - swing * 2 - 6.dp, 72.dp)
        val swingPx = with(LocalDensity.current) { swing.toPx() }
        items.forEachIndexed { i, choice ->
            val (duration, phase) = speeds[i]
            val t by transition.animateFloat(0f, 2f * PI.toFloat(), infiniteRepeatable(tween(duration, easing = LinearEasing), RepeatMode.Restart), label = "drift$i")
            val lone = i == items.lastIndex && items.size % 2 == 1
            val x = if (lone) maxWidth / 2 - w / 2 else cellW * (i % 2) + (cellW - w) / 2
            val y = cellH * (i / 2) + (cellH - h) / 2
            CompactAnswer(
                choice,
                ui,
                Modifier
                    .offset(x, y)
                    .offset { IntOffset((swingPx * sin(t + phase)).roundToInt(), (swingPx * cos(t * 1.3f + phase)).roundToInt()) }
                    .size(w, h),
            )
        }
    }
}

private val pebbleOutlines = listOf(
    RoundedCornerShape(46, 54, 42, 58),
    RoundedCornerShape(58, 42, 55, 45),
    RoundedCornerShape(40, 60, 60, 40),
)

/** The answers fall into a jar as pebbles, in rows of three from the bottom up. */
@Composable
internal fun PebbleJar(items: List<Choice>, seed: Long, ui: AnswerUi, modifier: Modifier = Modifier) {
    val looks = remember(seed, items.size) {
        val r = Random(seed)
        val slots = items.indices.shuffled(r)
        items.indices.map { slots[it] to (r.nextFloat() * 20f - 10f) }
    }
    val rows = ceil(items.size / 3f).toInt().coerceAtLeast(1)
    val outline = MaterialTheme.colorScheme.outlineVariant
    BoxWithConstraints(modifier.fillMaxSize()) {
        val w = min(maxWidth * 0.27f, 112.dp)
        val h = min(72.dp, (maxHeight - 30.dp) / (rows + 0.4f) - 8.dp).coerceAtLeast(34.dp)
        val slotCenters = items.indices.map { slot ->
            val row = slot / 3
            val inRow = minOf(3, items.size - row * 3)
            val x = when (inRow) {
                1 -> 0.5f
                2 -> listOf(0.33f, 0.67f)[slot % 3]
                else -> listOf(0.2f, 0.5f, 0.8f)[slot % 3]
            }
            x to maxHeight - h / 2 - 10.dp - (h + 8.dp) * row
        }
        val top = slotCenters.minOf { it.second }
        val rim = (top - h / 2 - 14.dp).coerceAtLeast(0.dp)
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * 0.03f
            val right = size.width * 0.97f
            val y = rim.toPx()
            val r = 48.dp.toPx()
            val path = Path().apply {
                moveTo(left, y)
                lineTo(left, size.height - r)
                quadraticTo(left, size.height, left + r, size.height)
                lineTo(right - r, size.height)
                quadraticTo(right, size.height, right, size.height - r)
                lineTo(right, y)
            }
            drawPath(path, outline, style = Stroke(3.dp.toPx()))
            drawRoundRect(outline, Offset(left - 8.dp.toPx(), y - 5.dp.toPx()), Size(right - left + 16.dp.toPx(), 10.dp.toPx()), CornerRadius(5.dp.toPx()))
        }
        val fallFrom = with(density) { -maxHeight.toPx() }
        items.forEachIndexed { i, choice ->
            val (slot, rotation) = looks[i]
            val (fx, cy) = slotCenters[slot]
            val fall = remember { Animatable(fallFrom) }
            LaunchedEffect(Unit) {
                delay(i * 90L)
                fall.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
            CompactAnswer(
                choice,
                ui,
                Modifier
                    .offset(maxWidth * fx - w / 2, cy - h / 2)
                    .graphicsLayer {
                        translationY = fall.value
                        rotationZ = rotation
                    }
                    .size(w, h),
                pebbleOutlines[i % pebbleOutlines.size],
            )
        }
    }
}

/** A ball rolls with the phone's tilt; the answer it touches is the answer. The tilt at the start counts as level. */
@Composable
internal fun TiltAnswers(items: List<Choice>, ui: AnswerUi, modifier: Modifier = Modifier) {
    val sensor = rememberTiltSensor {}
    val tap by rememberUpdatedState(ui.tap)
    val ballColor = MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(modifier.fillMaxSize()) {
        val leftCount = ceil(items.size / 2f).toInt().coerceAtLeast(1)
        val rows = leftCount
        val w = min(maxWidth * 0.34f, 132.dp)
        val h = min(64.dp, maxHeight / rows - 10.dp).coerceAtLeast(34.dp)
        val rowH = maxHeight / rows
        val places = items.mapIndexed { i, _ ->
            val left = i < leftCount
            val row = if (left) i else i - leftCount
            val x = if (left) 4.dp else maxWidth - w - 4.dp
            x to rowH * row + (rowH - h) / 2
        }
        val density = LocalDensity.current
        val radius = 16.dp
        val rects = with(density) { places.map { (x, y) -> Rect(Offset(x.toPx(), y.toPx()), Size(w.toPx(), h.toPx())) } }
        val bounds = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        var ball by remember { mutableStateOf(Offset(bounds.width / 2, bounds.height / 2)) }
        LaunchedEffect(bounds) {
            val r = with(density) { radius.toPx() }
            val pull = with(density) { 300.dp.toPx() }
            var velocity = Offset.Zero
            var position = Offset(bounds.width / 2, bounds.height / 2)
            delay(300)
            val level = Offset(sensor.ax, sensor.ay)
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                velocity = (velocity + (Offset(sensor.ax, sensor.ay) - level) * (pull * dt)) * (1f - 1.5f * dt)
                position += velocity * dt
                if (position.x < r || position.x > bounds.width - r) velocity = velocity.copy(x = -velocity.x * 0.5f)
                if (position.y < r || position.y > bounds.height - r) velocity = velocity.copy(y = -velocity.y * 0.5f)
                position = Offset(position.x.coerceIn(r, bounds.width - r), position.y.coerceIn(r, bounds.height - r))
                ball = position
                val hit = rects.indexOfFirst { it.inflate(-r / 2).contains(position) }
                if (hit >= 0) {
                    tap(items[hit])
                    break
                }
            }
        }
        items.forEachIndexed { i, choice ->
            val (x, y) = places[i]
            CompactAnswer(choice, ui, Modifier.offset(x, y).size(w, h), tappable = false)
        }
        Box(
            Modifier
                .offset { IntOffset((ball.x - radius.toPx()).roundToInt(), (ball.y - radius.toPx()).roundToInt()) }
                .size(radius * 2)
                .background(ballColor, CircleShape),
        )
    }
}

/** Answers sit in two columns; dragging one into the jar below answers it. */
@Composable
internal fun DragAnswers(items: List<Choice>, ui: AnswerUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tap by rememberUpdatedState(ui.tap)
    var over by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val jarW = min(maxWidth * 0.55f, 220.dp)
        val jarH = min(maxHeight * 0.28f, 110.dp)
        val area = maxHeight - jarH - 16.dp
        val rows = ceil(items.size / 2f).toInt().coerceAtLeast(1)
        val cellH = area / rows
        val h = min(56.dp, cellH - 8.dp).coerceAtLeast(34.dp)
        val w = maxWidth / 2 - 10.dp
        val density = LocalDensity.current
        val jar = with(density) { Rect(Offset(((maxWidth - jarW) / 2).toPx(), (maxHeight - jarH).toPx()), Size(jarW.toPx(), jarH.toPx())) }
        Box(
            Modifier
                .offset((maxWidth - jarW) / 2, maxHeight - jarH)
                .size(jarW, jarH)
                .background(if (over) ReasonColor.Ember.tones.container else colors.surfaceContainerHigh, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp, topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.prompt_drop_here), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
        items.forEachIndexed { i, choice ->
            val lone = i == items.lastIndex && items.size % 2 == 1
            val x = if (lone) (maxWidth - w) / 2 else (maxWidth / 2) * (i % 2) + 5.dp
            val y = cellH * (i / 2) + (cellH - h) / 2
            val home = with(density) { Offset(x.toPx(), y.toPx()) }
            val half = with(density) { Offset(w.toPx() / 2, h.toPx() / 2) }
            val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
            val scope = rememberCoroutineScope()
            CompactAnswer(
                choice,
                ui,
                Modifier
                    .offset(x, y)
                    .offset { IntOffset(drag.value.x.roundToInt(), drag.value.y.roundToInt()) }
                    .zIndex(if (drag.value != Offset.Zero) 1f else 0f)
                    .graphicsLayer { shadowElevation = if (drag.value != Offset.Zero) 12f else 0f }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, amount ->
                                change.consume()
                                scope.launch { drag.snapTo(drag.value + amount) }
                                over = jar.contains(home + half + drag.value + amount)
                            },
                            onDragEnd = {
                                val dropped = jar.contains(home + half + drag.value)
                                over = false
                                if (dropped) tap(choice) else scope.launch { drag.animateTo(Offset.Zero) }
                            },
                            onDragCancel = {
                                over = false
                                scope.launch { drag.animateTo(Offset.Zero) }
                            },
                        )
                    }
                    .size(w, h),
                tappable = false,
            )
        }
    }
}

/** Hold the circle for one slow breath before the answers wake up. Letting go early starts it again. */
@Composable
internal fun BreathGate(onDone: () -> Unit, onTooShort: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    val tooShort by rememberUpdatedState(onTooShort)
    val tones = ReasonColor.Ember.tones
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val size = min(maxWidth, maxHeight - 60.dp) * 0.8f
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(tones.container)
                    .semantics {
                        role = Role.Button
                        onClick { done(); true }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val job = scope.launch {
                                progress.animateTo(1f, tween(BREATH_MS, easing = LinearEasing))
                                done()
                            }
                            waitForUpOrCancellation()
                            if (job.isActive) {
                                job.cancel()
                                scope.launch { progress.snapTo(0f) }
                                tooShort()
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val s = 0.35f + 0.65f * sin(PI.toFloat() * progress.value)
                            scaleX = s
                            scaleY = s
                        }
                        .background(tones.ink.copy(alpha = 0.85f), CircleShape),
                )
            }
            val p = progress.value
            Text(
                stringResource(
                    when {
                        p == 0f -> R.string.prompt_breath_hold
                        p < 0.5f -> R.string.prompt_breath_in
                        else -> R.string.prompt_breath_out
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val BREATH_MS = 5_000

private class WildLook(
    val width: Float,
    val height: Dp,
    val outline: Int,
    val rotation: Float,
    val family: Int,
    val bold: Boolean,
    val upper: Boolean,
    val spaced: Boolean,
    val maxSp: Float,
    val color: ReasonColor,
    val pebble: PebbleShape,
)

private fun polygon(vararg points: Float): Shape = GenericShape { size, _ ->
    for (i in points.indices step 2) {
        val x = points[i] * size.width
        val y = points[i + 1] * size.height
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val wildOutlines: List<Pair<Shape, Float>> = listOf(
    RoundedCornerShape(50) to 0.1f,
    GenericShape { size, _ -> addOval(Rect(Offset.Zero, size)) } to 0.16f,
    RoundedCornerShape(3.dp) to 0.08f,
    RoundedCornerShape(topStartPercent = 50, topEndPercent = 8, bottomEndPercent = 50, bottomStartPercent = 8) to 0.12f,
    RoundedCornerShape(48, 30, 60, 22) to 0.12f,
    polygon(0.25f, 0f, 0.75f, 0f, 1f, 0.5f, 0.75f, 1f, 0.25f, 1f, 0f, 0.5f) to 0.2f,
    polygon(0.12f, 0f, 1f, 0f, 0.88f, 1f, 0f, 1f) to 0.15f,
    polygon(0.3f, 0f, 0.7f, 0f, 1f, 0.3f, 1f, 0.7f, 0.7f, 1f, 0.3f, 1f, 0f, 0.7f, 0f, 0.3f) to 0.14f,
    polygon(0f, 0f, 0.85f, 0f, 1f, 0.5f, 0.85f, 1f, 0f, 1f, 0.12f, 0.5f) to 0.17f,
)

private val wildFamilies = listOf(Bricolage, Figtree, FontFamily.Serif, FontFamily.Monospace)

/**
 * Every answer gets a new size, outline, tilt, font, color and pebble, the last two taken from another answer,
 * so only the label can be trusted. Sizes shrink together until every answer fits the space.
 */
@Composable
internal fun WildAnswers(items: List<Choice>, seed: Long, ui: AnswerUi, modifier: Modifier = Modifier) {
    val looks = remember(seed, items.size) {
        val r = Random(seed)
        val colors = items.map { it.style.color }.shuffled(r)
        val pebbles = items.map { it.style.shape }.shuffled(r)
        items.indices.map { i ->
            WildLook(
                width = 0.34f + r.nextFloat() * 0.36f,
                height = (56 + r.nextInt(95)).dp,
                outline = r.nextInt(wildOutlines.size),
                rotation = r.nextFloat() * 14f - 7f,
                family = r.nextInt(wildFamilies.size),
                bold = r.nextBoolean(),
                upper = r.nextFloat() < 0.35f,
                spaced = r.nextFloat() < 0.25f,
                maxSp = 14f + r.nextInt(17),
                color = colors[i],
                pebble = pebbles[i],
            )
        }
    }
    val gapX = 10.dp
    val gapY = 16.dp
    val baseStyle = MaterialTheme.typography.labelLarge
    val labels = items.mapIndexed { i, c -> ui.label(c).let { if (looks[i].upper) it.uppercase() else it } }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The smallest box each label fits at a readable size with whole words: its longest word sets the width.
    val needs = remember(looks, labels, density, baseStyle) {
        with(density) {
            labels.mapIndexed { i, label ->
                val look = looks[i]
                val inset = wildOutlines[look.outline].second
                val style = wildStyle(baseStyle, look).copy(fontSize = WILD_READABLE)
                val longest = label.split(' ').filter { it.isNotEmpty() }.maxOfOrNull { measurer.measure(it, style, softWrap = false).size.width } ?: 0
                val textW = longest + 2.dp.toPx()
                val textH = measurer.measure(label, style, constraints = Constraints(maxWidth = textW.roundToInt().coerceAtLeast(1))).size.height
                Size(textW / (1 - 2 * inset), (textH + WILD_MIN_ICON.toPx() + 6.dp.toPx()) / (1 - 2 * WILD_INSET_Y))
            }
        }
    }
    Layout(
        content = {
            items.forEachIndexed { i, choice ->
                val look = looks[i]
                val (shape, inset) = wildOutlines[look.outline]
                AnswerSurface(
                    shape = shape,
                    color = look.color.tones.container,
                    awake = ui.awake(choice),
                    hold = ui.hold,
                    onTap = { ui.tap(choice) },
                    modifier = Modifier.graphicsLayer { rotationZ = look.rotation },
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize().insetBy(inset, WILD_INSET_Y)) {
                        val icon = (maxHeight * 0.26f).coerceIn(WILD_MIN_ICON, 40.dp)
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        ) {
                            Pebble(PebbleStyle(look.pebble, look.color), icon)
                            FitText(labels[i], wildStyle(baseStyle, look), look.maxSp.sp, Modifier.weight(1f, fill = false), minSize = 10.sp)
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val gx = gapX.toPx()
        val gy = gapY.toPx()
        val raw = looks.map { Size(it.width * width, it.height.toPx()) }
        fun sized(scale: Float) = raw.mapIndexed { i, r ->
            Size((r.width * scale).coerceAtLeast(needs[i].width).coerceAtMost(width), (r.height * scale).coerceAtLeast(needs[i].height))
        }
        fun heightOf(sizes: List<Size>, rows: List<List<Int>>) =
            rows.sumOf { row -> row.maxOf { sizes[it].height }.toDouble() }.toFloat() + gy * (rows.size - 1)
        var scale = sqrt(0.5f * width * height / raw.sumOf { (it.width * it.height).toDouble() }.toFloat()).coerceAtMost(1.15f)
        var sizes = sized(scale)
        var rows = wildRows(sizes, width, gx)
        for (attempt in 0 until 12) {
            val total = heightOf(sizes, rows)
            if (total <= height) break
            scale *= height / total * 0.97f
            sizes = sized(scale)
            rows = wildRows(sizes, width, gx)
        }
        val placeables = measurables.mapIndexed { i, m ->
            m.measure(Constraints.fixed(sizes[i].width.roundToInt(), sizes[i].height.roundToInt()))
        }
        val total = rows.sumOf { row -> row.maxOf { sizes[it].height }.toDouble() }.toFloat() + gy * (rows.size - 1)
        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = ((height - total) / 2).coerceAtLeast(0f)
            rows.forEach { row ->
                val rowH = row.maxOf { sizes[it].height }
                val rowW = row.sumOf { sizes[it].width.toDouble() }.toFloat() + gx * (row.size - 1)
                var x = (width - rowW) / 2
                row.forEach { i ->
                    placeables[i].place(x.roundToInt(), (y + (rowH - sizes[i].height) / 2).roundToInt())
                    x += sizes[i].width + gx
                }
                y += rowH + gy
            }
        }
    }
}

private val WILD_READABLE = 12.sp
private val WILD_MIN_ICON = 14.dp
private const val WILD_INSET_Y = 0.08f

private fun wildStyle(base: TextStyle, look: WildLook): TextStyle = base.copy(
    fontFamily = wildFamilies[look.family],
    fontWeight = if (look.bold) FontWeight.ExtraBold else FontWeight.Medium,
    letterSpacing = if (look.spaced) 0.12.em else 0.em,
    lineHeight = 1.05.em,
)

/** Greedy rows: each answer goes on the current row while it fits the width. */
private fun wildRows(sizes: List<Size>, width: Float, gap: Float): List<List<Int>> {
    val rows = mutableListOf<MutableList<Int>>()
    var used = 0f
    sizes.forEachIndexed { i, size ->
        val w = size.width
        if (rows.isEmpty() || used + gap + w > width) {
            rows += mutableListOf(i)
            used = w
        } else {
            rows.last() += i
            used += gap + w
        }
    }
    return rows
}

/** Padding as a share of the own size, so text stays inside slanted and cut outlines at any size. */
private fun Modifier.insetBy(horizontal: Float, vertical: Float): Modifier = layout { measurable, constraints ->
    val px = (constraints.maxWidth * horizontal).roundToInt()
    val py = (constraints.maxHeight * vertical).roundToInt()
    val placeable = measurable.measure(constraints.offset(-2 * px, -2 * py))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(px, py) }
}
