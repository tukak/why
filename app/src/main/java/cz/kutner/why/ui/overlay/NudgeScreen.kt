package cz.kutner.why.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    val rise = remember { Animatable(1f) }
    LaunchedEffect(Unit) { rise.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = colors.background,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = rise.value * size.height },
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).background(colors.outlineVariant, RoundedCornerShape(2.dp)))
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
