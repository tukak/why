package cz.kutner.why.ui.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import cz.kutner.why.ui.components.ShapeBadge
import cz.kutner.why.ui.theme.PebbleStyle

@Composable
fun OfferScreen(label: String, count: Int, onDecide: (OfferDecision) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OverlaySheet(spacing = 20.dp) {
        ShapeBadge(PebbleStyle.Other, 64.dp, 22.dp, 34.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("“$label”", style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(pluralStringResource(R.plurals.offer_text, count, count), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        OfferButtons(stringResource(R.string.offer_add), stringResource(R.string.offer_never), onDecide)
    }
}

/** Add, Not now and Never, the same for every offer. */
@Composable
internal fun ColumnScope.OfferButtons(addLabel: String, neverLabel: String, onDecide: (OfferDecision) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { onDecide(OfferDecision.Add) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(addLabel, style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalButton(onClick = { onDecide(OfferDecision.NotNow) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.offer_not_now), style = MaterialTheme.typography.labelLarge)
        }
    }
    TextButton(onClick = { onDecide(OfferDecision.Never) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(neverLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
