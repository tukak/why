package cz.kutner.why.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.PromptChoices
import cz.kutner.why.data.settings.AppSettings
import cz.kutner.why.domain.TimeOfDayOrder
import cz.kutner.why.domain.Trick
import cz.kutner.why.domain.TrickGroup
import cz.kutner.why.domain.TrickSetup
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.description
import cz.kutner.why.ui.overlay.PromptScreen
import cz.kutner.why.ui.theme.ReasonColor
import cz.kutner.why.ui.timeFormatter
import cz.kutner.why.ui.title
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.launch

@Composable
fun AnnoyScreen(onBack: () -> Unit, onTry: (Trick) -> Unit) {
    val repo = LocalContext.current.container.settings
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val on = settings.annoyingMode
    val total = Trick.entries.size

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val backLabel = stringResource(R.string.back)
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backLabel }) {
                LineIcon(Icons.ArrowLeft, colors.onSecondaryContainer)
            }
            Text(stringResource(R.string.annoy_title), style = MaterialTheme.typography.headlineMedium)
        }

        val tones = ReasonColor.Ember.tones
        Column(
            Modifier.fillMaxWidth().background(tones.container, RoundedCornerShape(28.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (on) R.string.annoy_on else R.string.annoy_off), style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (on) stringResource(R.string.annoy_count, settings.tricksOn.size, total) else stringResource(R.string.annoy_turn_on, total),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onTertiaryContainer,
                    )
                }
                val label = stringResource(R.string.annoy_title)
                Switch(checked = on, onCheckedChange = { scope.launch { repo.setAnnoyingMode(it) } }, modifier = Modifier.semantics { contentDescription = label })
            }
            Text(stringResource(R.string.annoy_explain), style = MaterialTheme.typography.bodyMedium, color = colors.onTertiaryContainer)
        }

        TrickGroup.entries.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(group.title),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Column(Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    val tricks = Trick.entries.filter { it.group == group }
                    tricks.forEachIndexed { i, trick ->
                        TrickRow(
                            trick = trick,
                            checked = trick in settings.tricksOn,
                            enabled = on,
                            onCheck = { scope.launch { repo.setTrickOn(trick, it) } },
                            onTry = { onTry(trick) },
                        )
                        if (i < tricks.lastIndex) HorizontalDivider(color = colors.surfaceContainerHigh)
                    }
                }
            }
        }
        Text(stringResource(R.string.annoy_skipped), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun TrickRow(trick: Trick, checked: Boolean, enabled: Boolean, onCheck: (Boolean) -> Unit, onTry: () -> Unit) {
    val name = stringResource(trick.title)
    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(trick.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val tryLabel = stringResource(R.string.annoy_try_label, name)
        FilledTonalButton(onClick = onTry, modifier = Modifier.semantics { contentDescription = tryLabel }) { Text(stringResource(R.string.annoy_try)) }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheck, modifier = Modifier.semantics { contentDescription = name })
    }
}

/** The question screen with one trick and the real reasons, to see the trick without an unlock. Any answer goes back. */
@Composable
fun TryTrickScreen(trick: Trick, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.container
    val now = remember { app.clock.millis() }
    val loaded by produceState<Pair<PromptChoices, Int>?>(null) {
        val choices = app.unlocks.promptChoices(TimeOfDayOrder(now, app.clock.zone), answersSince = now - 30.days.inWholeMilliseconds)
        value = choices to app.unlocks.countSince(startOfDay(now, app.clock.zone))
    }
    val (choices, count) = loaded ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val setup = remember(choices) { TrickSetup.create(trick, choices.reasons.size, Random.Default) }
    PromptScreen(
        time = timeFormatter(context).format(Instant.ofEpochMilli(now).atZone(app.clock.zone)),
        unlockNumber = count,
        reasons = choices.reasons,
        typedBefore = choices.typed,
        setup = setup,
        showHint = true,
        onAnswer = { _, _ -> onBack() },
        onOpenApp = onBack,
        onPause = onBack,
    )
}
