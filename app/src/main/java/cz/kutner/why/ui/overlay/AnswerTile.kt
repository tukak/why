package cz.kutner.why.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import cz.kutner.why.data.db.Reason
import cz.kutner.why.ui.theme.PebbleStyle
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** One answer on the question screen: a reason, or "Just habit" when [reason] is null. [index] is its number in a [cz.kutner.why.domain.TrickSetup]. */
data class Choice(val index: Int, val label: String, val style: PebbleStyle, val reason: Reason?)

/** How a tap on a sleeping answer looks: visible enough to read and decide, but clearly not ready. */
internal const val ASLEEP_ALPHA = 0.38f
internal const val HOLD_MS = 650

/**
 * The tappable body of an answer in any layout. A tap on a sleeping answer still reaches [onTap], which counts it.
 * With [hold], the answer needs a press of [HOLD_MS]; a screen reader's double tap still answers.
 * Without [tappable], the layout handles input itself (drag, tilt).
 */
@Composable
internal fun AnswerSurface(
    shape: Shape,
    color: Color,
    awake: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    hold: Boolean = false,
    tappable: Boolean = true,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha by animateFloatAsState(if (awake) 1f else ASLEEP_ALPHA, label = "awake")
    val tap by rememberUpdatedState(onTap)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val fill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val input = when {
        !tappable -> Modifier
        hold -> Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick { tap(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch {
                        progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                        tap()
                    }
                    waitForUpOrCancellation()
                    if (job.isActive) job.cancel()
                    scope.launch { progress.snapTo(0f) }
                }
            }
        else -> Modifier.clickable(role = Role.Button) { tap() }
    }
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(color)
            .drawWithContent {
                drawContent()
                if (progress.value > 0f) drawRect(fill, size = size.copy(width = size.width * progress.value))
            }
            .then(input),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
    }
}

/** Text that shrinks until it fits its box with every word whole, so a label is never cut in a fixed-size answer. */
@Composable
internal fun FitText(text: String, style: TextStyle, maxSize: TextUnit, modifier: Modifier = Modifier, minSize: TextUnit = 9.sp) {
    BasicText(
        text,
        modifier,
        style = style.copy(color = LocalContentColor.current, textAlign = TextAlign.Center),
        autoSize = WholeWords(minSize, maxSize),
    )
}

/** Like a stepped auto size, but a size that breaks a word inside counts as too big. */
private data class WholeWords(val min: TextUnit, val max: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        // Candidate i is max - i * STEP; the largest size that fits is the smallest i that fits.
        val count = ceil((max.value - min.value) / STEP).toInt()
        fun fits(i: Int): Boolean {
            val layout = performLayout(constraints, text, (max.value - i * STEP).sp)
            return !layout.hasVisualOverflow && layout.keepsWords(text.text)
        }
        var lo = 0
        var hi = count
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (fits(mid)) hi = mid else lo = mid + 1
        }
        return if (lo < count) (max.value - lo * STEP).sp else min
    }
}

private const val STEP = 0.5f

private fun TextLayoutResult.keepsWords(text: String): Boolean = (0 until lineCount - 1).all { line ->
    val end = getLineEnd(line)
    end <= 0 || end >= text.length || text[end - 1].isWhitespace() || text[end].isWhitespace() || text[end - 1] == '-'
}
