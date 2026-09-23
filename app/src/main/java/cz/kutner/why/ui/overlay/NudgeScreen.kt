package cz.kutner.why.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cz.kutner.why.R
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleStyle

@Composable
fun NudgeScreen(
    minutes: Int,
    reasonLabel: String?,
    style: PebbleStyle,
    onDone: () -> Unit,
    onMore: () -> Unit,
    onSwitched: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OverlaySheet(spacing = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(72.dp).background(style.color.tones.container, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
                Pebble(style, 40.dp)
            }
            Column {
                Text(pluralStringResource(R.plurals.nudge_minutes, minutes, minutes), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.nudge_since), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Text(nudgeMessage(reasonLabel, style), style = MaterialTheme.typography.bodyLarge)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.nudge_done), style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(onClick = onMore, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.nudge_more), style = MaterialTheme.typography.labelLarge)
            }
        }
        TextButton(onClick = onSwitched, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.nudge_switched), color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun nudgeMessage(reasonLabel: String?, style: PebbleStyle) = if (style == PebbleStyle.Habit || reasonLabel == null) {
    buildAnnotatedString { append(stringResource(R.string.nudge_habit)) }
} else {
    val template = stringResource(R.string.nudge_reason, "\u0000")
    val (before, after) = template.split("\u0000").let { it[0] to it.getOrElse(1) { "" } }
    buildAnnotatedString {
        append(before)
        withStyle(SpanStyle(color = style.color.tones.ink, fontWeight = FontWeight.Bold)) { append(reasonLabel) }
        append(after)
    }
}
