package cz.kutner.why.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.kutner.why.R
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.TypedReasons
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.style
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import kotlinx.coroutines.delay

@Composable
fun PromptScreen(
    time: String,
    unlockNumber: Int,
    reasons: List<Reason>,
    typedBefore: List<String>,
    onReason: (Reason) -> Unit,
    onHabit: () -> Unit,
    onOther: (String) -> Unit,
    onOpenApp: () -> Unit,
    onPause: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(Modifier.padding(top = 24.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.prompt_header, time, unlockNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .background(colors.surfaceContainerHigh, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                val openLabel = stringResource(R.string.prompt_open_app)
                FilledTonalIconButton(onClick = onOpenApp, modifier = Modifier.semantics { contentDescription = openLabel }) {
                    LineIcon(Icons.Jar, colors.onSecondaryContainer)
                }
            }
            Text(stringResource(R.string.prompt_title), style = MaterialTheme.typography.displaySmall, color = colors.onSurface)
        }

        // Only the reasons scroll, so "Just habit" and "Something else" stay reachable with any number of reasons.
        val grid = rememberScrollState()
        Box(Modifier.weight(1f, fill = false)) {
            Column(Modifier.verticalScroll(grid), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reasons.chunked(2).forEachIndexed { row, pair ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { col, reason ->
                            ReasonChip(reason, index = row * 2 + col, onClick = { onReason(reason) }, modifier = Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
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

        Spacer(Modifier.height(16.dp))
        HabitButton(onHabit)
        Spacer(Modifier.height(16.dp))
        OtherReasonField(typedBefore, onOther)
        TextButton(onClick = onPause, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)) {
            Text(stringResource(R.string.prompt_pause), color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReasonChip(reason: Reason, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = reason.style
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 15L)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = style.color.tones.container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .heightIn(min = 92.dp)
            .fillMaxHeight()
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationY = (1f - appear.value) * 40f
                scaleX = 0.94f + 0.06f * appear.value
                scaleY = scaleX
            },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Pebble(style, 30.dp)
            Text(reason.label, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HabitButton(onClick: () -> Unit) {
    // One turn on appear; a never-ending spin would redraw the window all the time.
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) { angle.animateTo(360f, tween(1_400, easing = FastOutSlowInEasing)) }
    val tones = ReasonColor.Ember.tones
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = tones.container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Pebble(PebbleStyle.Habit, 40.dp, Modifier.graphicsLayer { rotationZ = angle.value })
            Column {
                Text(stringResource(R.string.habit), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.habit_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherReasonField(typedBefore: List<String>, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val submit = { if (text.isNotBlank()) onSubmit(text) }
    val saveLabel = stringResource(R.string.prompt_save)
    val shown = typedBefore.filter { TypedReasons.matches(it, text) && TypedReasons.normalize(it) != TypedReasons.normalize(text) }.take(SUGGESTIONS_SHOWN)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.prompt_other_label), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.prompt_other_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerLowest,
                    unfocusedContainerColor = colors.surfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
            )
            FilledIconButton(
                onClick = submit,
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.size(56.dp).semantics { contentDescription = saveLabel },
            ) {
                Box { LineIcon(Icons.ArrowRight, colors.onPrimary) }
            }
        }
        // Below the field, so the field does not move under the finger when the chips appear.
        AnimatedVisibility(focused && shown.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEach { earlier ->
                    Surface(
                        onClick = { onSubmit(earlier) },
                        shape = RoundedCornerShape(50),
                        color = PebbleStyle.Other.color.tones.container,
                        contentColor = colors.onSurface,
                    ) {
                        Row(
                            Modifier.heightIn(min = 44.dp).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Pebble(PebbleStyle.Other, 14.dp)
                            Text(earlier, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private const val SUGGESTIONS_SHOWN = 6
