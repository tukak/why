package cz.kutner.why.ui.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.kutner.why.R
import cz.kutner.why.data.OfferDecision
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleStyle

@Composable
fun OfferScreen(label: String, count: Int, onDecide: (OfferDecision) -> Unit) {
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).background(colors.outlineVariant, RoundedCornerShape(2.dp)))
                Box(Modifier.size(64.dp).background(PebbleStyle.Other.color.tones.container, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Pebble(PebbleStyle.Other, 34.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("“$label”", style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(pluralStringResource(R.plurals.offer_text, count, count), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onDecide(OfferDecision.Add) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(stringResource(R.string.offer_add), style = MaterialTheme.typography.labelLarge)
                    }
                    FilledTonalButton(onClick = { onDecide(OfferDecision.NotNow) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text(stringResource(R.string.offer_not_now), style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = { onDecide(OfferDecision.Never) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.offer_never), color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
