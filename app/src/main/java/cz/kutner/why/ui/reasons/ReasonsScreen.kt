package cz.kutner.why.ui.reasons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.OfferDecision
import cz.kutner.why.data.db.Reason
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.style
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor

/** Editor target: an existing reason, or a new one when [reason] is null. */
private data class Editing(val reason: Reason?)

@Composable
fun ReasonsScreen() {
    val app = LocalContext.current.container
    val vm = viewModel { ReasonsViewModel(app.unlocks, app.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.reasons_title), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.reasons_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }

            Column(Modifier.background(colors.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(vertical = 4.dp)) {
                state.active.forEach { row -> ReasonListRow(row) { editing = Editing(row.reason) } }
                HabitRow()
            }

            state.offer?.let { offer -> OfferCard(offer.label, offer.count) { vm.decide(offer, it) } }

            if (state.archived.isNotEmpty()) {
                TextButton(onClick = { showArchived = !showArchived }) {
                    Text(pluralStringResource(R.plurals.reasons_archived, state.archived.size, state.archived.size), color = colors.onSurfaceVariant)
                }
                if (showArchived) {
                    state.archived.forEach { reason ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Pebble(reason.style, 20.dp)
                            Text(reason.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
                            TextButton(onClick = { vm.setArchived(reason, false) }) { Text(stringResource(R.string.reasons_restore)) }
                        }
                    }
                }
            }
        }

        val addLabel = stringResource(R.string.reasons_new)
        FloatingActionButton(
            onClick = { editing = Editing(null) },
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(64.dp).semantics { contentDescription = addLabel },
        ) {
            LineIcon(Icons.Plus, colors.onPrimary, size = 26.dp, strokeWidth = 2.4f)
        }
    }

    editing?.let { target ->
        ReasonEditor(
            reason = target.reason,
            onDismiss = { editing = null },
            onSave = { label, shape, color ->
                val existing = target.reason
                if (existing == null) vm.add(label, shape, color) else vm.save(existing.copy(label = label, shape = shape.name, color = color.name))
                editing = null
            },
            onArchive = {
                target.reason?.let { vm.setArchived(it, true) }
                editing = null
            },
        )
    }
}

@Composable
private fun ReasonListRow(row: ReasonRow, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val editLabel = stringResource(R.string.reasons_edit, row.reason.label)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onEdit).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShapeBadge(row.reason.style)
        Text(row.reason.label, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.reasons_count, row.count), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        IconButton(onClick = onEdit, modifier = Modifier.semantics { contentDescription = editLabel }) {
            LineIcon(Icons.Pencil, colors.onSurfaceVariant, size = 18.dp)
        }
    }
}

@Composable
private fun HabitRow() {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShapeBadge(PebbleStyle.Habit)
        Text(stringResource(R.string.habit), style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.reasons_always_shown), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShapeBadge(style: PebbleStyle) {
    Box(Modifier.size(36.dp).background(style.color.tones.container, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        Pebble(style, 20.dp)
    }
}

@Composable
private fun OfferCard(label: String, count: Int, onDecide: (OfferDecision) -> Unit) {
    val tones = ReasonColor.Violet.tones
    Column(
        Modifier.fillMaxWidth().background(tones.container, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.reasons_suggested), style = MaterialTheme.typography.labelSmall, color = tones.ink)
        Text("“$label”", style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(pluralStringResource(R.plurals.offer_text, count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onDecide(OfferDecision.Add) }) { Text(stringResource(R.string.reasons_add)) }
            FilledTonalButton(onClick = { onDecide(OfferDecision.NotNow) }) { Text(stringResource(R.string.offer_not_now)) }
        }
        TextButton(onClick = { onDecide(OfferDecision.Never) }) { Text(stringResource(R.string.offer_never), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ReasonEditor(
    reason: Reason?,
    onDismiss: () -> Unit,
    onSave: (String, PebbleShape, ReasonColor) -> Unit,
    onArchive: () -> Unit,
) {
    var label by rememberSaveable { mutableStateOf(reason?.label.orEmpty()) }
    var shape by remember { mutableStateOf(reason?.let { PebbleShape.of(it.shape) } ?: PebbleShape.pickable.first()) }
    var color by remember { mutableStateOf(reason?.let { ReasonColor.of(it.color) } ?: ReasonColor.pickable.first()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(if (reason == null) R.string.reasons_new else R.string.reasons_edit_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.reasons_label)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Picker(PebbleShape.pickable, selected = shape, onPick = { shape = it }) { Pebble(PebbleStyle(it, color), 26.dp) }
            Picker(ReasonColor.pickable, selected = color, onPick = { color = it }) { Box(Modifier.size(26.dp).background(it.tones.ink, RoundedCornerShape(50))) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reason != null) {
                    FilledTonalButton(onClick = onArchive, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.reasons_archive)) }
                }
                Button(onClick = { onSave(label, shape, color) }, enabled = label.isNotBlank(), modifier = Modifier.weight(1f).height(52.dp)) {
                    Text(stringResource(R.string.reasons_save))
                }
            }
        }
    }
}

@Composable
private fun <T> Picker(options: List<T>, selected: T, onPick: (T) -> Unit, item: @Composable (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        options.forEach { option ->
            Box(
                Modifier
                    .size(44.dp)
                    .border(3.dp, if (option == selected) MaterialTheme.colorScheme.tertiary else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable { onPick(option) },
                contentAlignment = Alignment.Center,
            ) { item(option) }
        }
    }
}
