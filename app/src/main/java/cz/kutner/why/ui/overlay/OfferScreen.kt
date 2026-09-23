package cz.kutner.why.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.kutner.why.R
import cz.kutner.why.data.OfferDecision
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleStyle

@Composable
fun OfferScreen(label: String, count: Int, onDecide: (OfferDecision) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OverlaySheet(spacing = 20.dp) {
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
