package cz.kutner.why.ui.overlay

import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kutner.why.R
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.GridKind
import cz.kutner.why.domain.Trick
import cz.kutner.why.domain.TrickSetup
import cz.kutner.why.domain.TypedReasons
import cz.kutner.why.domain.scrambled
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.description
import cz.kutner.why.ui.questions
import cz.kutner.why.ui.style
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.title
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The question shown at unlock. With a [setup], annoying mode changes the screen by one trick; [showHint] explains
 * the trick the first time it appears.
 */
@Composable
fun PromptScreen(
    time: String,
    unlockNumber: Int,
    reasons: List<Reason>,
    typedBefore: List<String>,
    setup: TrickSetup?,
    showHint: Boolean,
    onAnswer: (PromptAnswer, AnswerStats) -> Unit,
    onOpenApp: () -> Unit,
    onPause: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val usable = setup?.takeIf { it.reasons == reasons.size && reasons.size + 1 <= it.trick.maxAnswers }
    val state = remember(usable) { PromptState(usable) }
    val trick = state.trick
    val habitLabel = stringResource(R.string.habit)
    val choices = remember(reasons, habitLabel) {
        reasons.mapIndexed { i, r -> Choice(i, r.label, r.style, r) } + Choice(reasons.size, habitLabel, PebbleStyle.Habit, null)
    }
    val ordered = usable?.order?.map { choices[it] } ?: choices
    val scramble = rememberScramble(state, choices)
    GateEffects(state)

    val deliver: (Pair<PromptAnswer, AnswerStats>?) -> Unit = { result -> if (result != null) onAnswer(result.first, result.second) }
    val ui = AnswerUi(
        awake = { state.awake(it.index) },
        tap = { c -> deliver(state.tap(c.index, c.reason?.let { PromptAnswer.Picked(it) } ?: PromptAnswer.Habit, c.label)) },
        label = { c -> scramble(c) },
        hold = trick == Trick.Hold,
    )
    val submitText = { text: String -> deliver(state.tap(PromptState.TYPED, PromptAnswer.Typed(text.trim()), text.trim())) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                BasicText(
                    stringResource(if (trick == Trick.Question && usable != null) questions[usable.question] else R.string.prompt_title),
                    style = MaterialTheme.typography.displaySmall.copy(color = colors.onSurface),
                    maxLines = 2,
                    autoSize = TextAutoSize.StepBased(minFontSize = 26.sp, maxFontSize = MaterialTheme.typography.displaySmall.fontSize),
                )
                if (showHint && trick != null) TrickHint(trick)
                state.message?.let { Text(stringResource(it), style = MaterialTheme.typography.titleSmall, color = colors.error) }
            }

            val fills = trick in FILLING || (trick == Trick.NewGrid && usable?.gridKind == GridKind.Circle) || (trick == Trick.Breath && state.openAt == null)
            Box(Modifier.weight(1f, fill = fills).fillMaxWidth()) {
                Answers(state, usable, ordered, ui)
            }

            Spacer(Modifier.height(16.dp))
            OtherReasonField(
                typedBefore = typedBefore,
                text = state.typed,
                onText = { state.typed = it },
                enabled = state.awake(PromptState.TYPED),
                filter = trick == Trick.TypeIt,
                onSubmit = submitText,
            )
            TextButton(onClick = onPause, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)) {
                Text(stringResource(R.string.prompt_pause), color = colors.onSurfaceVariant)
            }
        }
        state.pending?.let { pending ->
            ConfirmSheet(
                label = pending.label,
                onYes = { onAnswer(pending.answer, pending.stats) },
                onNo = { state.pending = null },
            )
        }
    }
}

/** Layouts that need all the room between the title and the text field. */
private val FILLING = setOf(Trick.Wild, Trick.Drift, Trick.Pebbles, Trick.Tilt, Trick.DragToJar)

@Composable
private fun Answers(state: PromptState, setup: TrickSetup?, ordered: List<Choice>, ui: AnswerUi) {
    when {
        setup == null -> GridAnswers(ordered.dropLast(1), ordered.last(), 2, false, ui)
        setup.trick == Trick.Breath && state.openAt == null -> BreathGate(
            onDone = state::open,
            onTooShort = { state.message = R.string.prompt_breath_again },
        )
        setup.trick == Trick.Wild -> WildAnswers(ordered, setup.seed, ui)
        setup.trick == Trick.Drift -> DriftAnswers(ordered, setup.seed, ui)
        setup.trick == Trick.Pebbles -> PebbleJar(ordered, setup.seed, ui)
        setup.trick == Trick.Tilt -> TiltAnswers(ordered, ui)
        setup.trick == Trick.DragToJar -> DragAnswers(ordered, ui)
        setup.trick == Trick.NewGrid -> when (setup.gridKind) {
            GridKind.Circle -> CircleAnswers(ordered, ui)
            GridKind.ThreeColumns -> GridAnswers(ordered, null, 3, false, ui)
            GridKind.List -> GridAnswers(ordered, null, 1, true, ui)
        }
        setup.trick == Trick.TypeIt -> {
            val query = state.typed.trim()
            val found = if (query.length < 2) emptyList() else ordered.filter { it.label.contains(query, ignoreCase = true) }
            if (found.isEmpty()) {
                TypeHelp(if (query.length < 2) R.string.prompt_type_help else R.string.prompt_type_no_match)
            } else {
                GridAnswers(found, null, 2, false, ui)
            }
        }
        else -> GridAnswers(ordered.dropLast(1), ordered.last(), 2, false, ui)
    }
}

/** Starts the waits of the timing tricks. */
@Composable
private fun GateEffects(state: PromptState) {
    val setup = state.setup ?: return
    when (setup.trick) {
        Trick.Pause, Trick.EarlyReset -> LaunchedEffect(state.pauseRound) {
            delay(setup.pauseMs)
            state.open()
        }
        Trick.Stagger -> LaunchedEffect(Unit) {
            setup.wakeMs.forEach { (index, ms) ->
                launch {
                    delay(ms)
                    state.wake(index)
                }
            }
        }
        else -> Unit
    }
}

/** Mixes the labels while the scramble trick runs and wakes the answers once they are readable. */
@Composable
private fun rememberScramble(state: PromptState, choices: List<Choice>): (Choice) -> String {
    val setup = state.setup
    if (setup?.trick != Trick.Scramble) return { it.label }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(TrickSetup.SCRAMBLE_MS.toInt(), easing = LinearEasing))
        state.open()
    }
    val mixes = remember(choices) {
        choices.map { c ->
            val r = Random(setup.seed + c.index)
            c.label.indices.shuffled(r) to c.label.indices.shuffled(r)
        }
    }
    val p = progress.value
    return { c -> mixes[c.index].let { (order, source) -> scrambled(c.label, order, source, p) } }
}

@Composable
private fun TrickHint(trick: Trick) {
    val colors = MaterialTheme.colorScheme
    val name = stringResource(trick.title)
    val text = stringResource(R.string.prompt_new_trick, name, stringResource(trick.description))
    Surface(color = colors.inverseSurface, contentColor = colors.inverseOnSurface, shape = RoundedCornerShape(14.dp)) {
        Text(
            buildAnnotatedString {
                val at = text.indexOf(name)
                if (at < 0) {
                    append(text)
                } else {
                    append(text.substring(0, at))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(name) }
                    append(text.substring(at + name.length))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TypeHelp(text: Int) {
    val colors = MaterialTheme.colorScheme
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
            .padding(18.dp),
    )
}

@Composable
private fun ConfirmSheet(label: String, onYes: () -> Unit, onNo: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.BottomCenter) {
        Surface(color = colors.background, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.prompt_confirm, label), style = MaterialTheme.typography.headlineSmall)
                Button(onClick = onYes, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.prompt_confirm_yes), style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(onClick = onNo, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.prompt_confirm_no), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** With [filter], the field finds the answers (annoying mode's "Type it") and has the focus from the start. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OtherReasonField(
    typedBefore: List<String>,
    text: String,
    onText: (String) -> Unit,
    enabled: Boolean,
    filter: Boolean,
    onSubmit: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val submit = { if (text.isNotBlank()) onSubmit(text) }
    val saveLabel = stringResource(R.string.prompt_save)
    val focus = remember { FocusRequester() }
    if (filter) LaunchedEffect(Unit) { focus.requestFocus() }
    val shown = remember(typedBefore, text) {
        val typed = TypedReasons.normalize(text)
        typedBefore.filter { TypedReasons.matches(it, text) && TypedReasons.normalize(it) != typed }.take(SUGGESTIONS_SHOWN)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (filter) R.string.prompt_type_label else R.string.prompt_other_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = onText,
                enabled = enabled,
                placeholder = { Text(stringResource(if (filter) R.string.prompt_type_placeholder else R.string.prompt_other_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerLowest,
                    unfocusedContainerColor = colors.surfaceContainerLowest,
                    disabledContainerColor = colors.surfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused }
                    .then(if (filter) Modifier.border(2.dp, PebbleStyle.Habit.color.tones.ink, RoundedCornerShape(28.dp)) else Modifier),
            )
            FilledIconButton(
                onClick = submit,
                enabled = enabled && text.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                modifier = Modifier.size(56.dp).semantics { contentDescription = saveLabel },
            ) {
                LineIcon(Icons.ArrowRight, colors.onPrimary)
            }
        }
        // Below the field, so the field does not move under the finger when the chips appear.
        CapturedAnimatedVisibility(
            visible = enabled && focused && !filter && shown.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
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
