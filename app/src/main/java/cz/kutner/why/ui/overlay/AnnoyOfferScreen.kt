package cz.kutner.why.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cz.kutner.why.R
import cz.kutner.why.data.OfferDecision
import cz.kutner.why.ui.components.ShapeBadge
import cz.kutner.why.ui.theme.PebbleStyle

/** Offers annoying mode after many fast answers. The dots show the last [total] answers, the [fast] ones filled. */
@Composable
fun AnnoyOfferScreen(fast: Int, total: Int, onDecide: (OfferDecision) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ink = PebbleStyle.Habit.color.tones.ink
    OverlaySheet(spacing = 18.dp) {
        ShapeBadge(PebbleStyle.Habit, 64.dp, 22.dp, 34.dp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.annoy_offer_title), style = MaterialTheme.typography.headlineSmall)
            val counts = pluralStringResource(R.plurals.annoy_offer_count, fast, fast, total)
            Text(counts, style = MaterialTheme.typography.bodyLarge)
            Column(Modifier.semantics(mergeDescendants = true) { contentDescription = counts }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until total).chunked(15).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { i ->
                            val dot = Modifier.weight(1f).aspectRatio(1f)
                            // Fast answers first: the count is what matters, not their order.
                            if (i < fast) Box(dot.background(ink, CircleShape)) else Box(dot.border(2.dp, colors.outlineVariant, CircleShape))
                        }
                    }
                }
            }
            Text(stringResource(R.string.annoy_offer_text), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        OfferButtons(stringResource(R.string.annoy_offer_add), stringResource(R.string.annoy_offer_never), onDecide)
    }
}
