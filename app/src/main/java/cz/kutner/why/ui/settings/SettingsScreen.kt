package cz.kutner.why.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.settings.AppSettings
import cz.kutner.why.data.settings.ThemeMode
import cz.kutner.why.domain.formatMinuteOfDay
import cz.kutner.why.domain.parseMinuteOfDay
import cz.kutner.why.ui.components.BackgroundHelp
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import cz.kutner.why.ui.timeFormatter
import java.time.LocalTime
import kotlinx.coroutines.launch

/** Replace with the real page before release. */
private const val DONATE_URL = "https://ko-fi.com/tukak"

private enum class CustomSetting { Nudge, Resume, Reflection, DayStart, DayEnd }

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.container.settings
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<CustomSetting?>(null) }

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
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        }

        Section(stringResource(R.string.settings_appearance)) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOption(mode, selected = settings.themeMode == mode, modifier = Modifier.weight(1f)) {
                        scope.launch { repo.setThemeMode(mode) }
                    }
                }
            }
            Text(
                stringResource(
                    when (settings.themeMode) {
                        ThemeMode.System -> R.string.settings_theme_system_hint
                        ThemeMode.Light -> R.string.settings_theme_light_hint
                        ThemeMode.Dark -> R.string.settings_theme_dark_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            SwitchRow(
                title = stringResource(R.string.settings_wallpaper),
                subtitle = stringResource(R.string.settings_wallpaper_hint),
                checked = settings.dynamicColor,
                onChange = { scope.launch { repo.setDynamicColor(it) } },
            )
        }

        Section(stringResource(R.string.settings_checkins)) {
            Text(stringResource(R.string.settings_nudge_after), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                listOf(5, 10, 15, 0),
                selected = settings.nudgeMinutes,
                onSelect = { scope.launch { repo.setNudgeMinutes(it) } },
                onCustom = { editing = CustomSetting.Nudge },
            ) {
                if (it == 0) stringResource(R.string.settings_off) else stringResource(R.string.duration_minutes, it.toLong())
            }
            Column {
                Text(stringResource(R.string.settings_resume), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.settings_resume_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            ChoiceChips(
                listOf(0, 3, 10, 30),
                selected = settings.resumeSeconds,
                onSelect = { scope.launch { repo.setResumeSeconds(it) } },
                onCustom = { editing = CustomSetting.Resume },
            ) {
                if (it == 0) stringResource(R.string.settings_off) else stringResource(R.string.duration_seconds, it)
            }
            SwitchRow(
                title = stringResource(R.string.settings_reflection),
                subtitle = stringResource(R.string.settings_reflection_hint),
                checked = settings.reflectionEnabled,
                onChange = { scope.launch { repo.setReflectionEnabled(it) } },
            )
            if (settings.reflectionEnabled) {
                val locale = LocalLocale.current.platformLocale
                val timeFormat = remember(locale) { timeFormatter(context, locale) }
                ChoiceChips(
                    listOf(20, 21, 22, 23).map { it * 60 },
                    selected = settings.reflectionMinute,
                    onSelect = { scope.launch { repo.setReflectionMinute(it) } },
                    onCustom = { editing = CustomSetting.Reflection },
                ) {
                    LocalTime.of(it / 60, it % 60).format(timeFormat)
                }
            }
        }

        Section(stringResource(R.string.settings_day)) {
            Text(stringResource(R.string.settings_day_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            DayTimeRow(
                title = stringResource(R.string.settings_day_start),
                value = formatMinuteOfDay(settings.dayStartMinute),
                onChange = { editing = CustomSetting.DayStart },
            )
            DayTimeRow(
                title = stringResource(R.string.settings_day_end),
                value = formatMinuteOfDay(settings.dayEndMinute),
                onChange = { editing = CustomSetting.DayEnd },
            )
        }

        var unrestricted by remember { mutableStateOf(BackgroundHelp.isUnrestricted(context)) }
        LifecycleResumeEffect(Unit) {
            unrestricted = BackgroundHelp.isUnrestricted(context)
            onPauseOrDispose { }
        }
        Section(stringResource(R.string.settings_background)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.onboarding_battery), style = MaterialTheme.typography.labelLarge)
                    Text(
                        stringResource(if (unrestricted) R.string.settings_battery_ok else R.string.onboarding_battery_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (!unrestricted) {
                    FilledTonalButton(onClick = { BackgroundHelp.openAppSettings(context) }) { Text(stringResource(R.string.onboarding_allow)) }
                }
            }
            TextButton(onClick = { BackgroundHelp.openManufacturerTips(context) }) {
                Text(stringResource(R.string.background_tips, BackgroundHelp.manufacturer))
            }
        }

        Row(
            Modifier.fillMaxWidth().background(ReasonColor.Ember.tones.container, RoundedCornerShape(28.dp)).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(0.8f)) {
                Text(stringResource(R.string.settings_free_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_free_text), style = MaterialTheme.typography.bodySmall, color = colors.onTertiaryContainer)
            }
            Button(
                onClick = { BackgroundHelp.open(context, Intent(Intent.ACTION_VIEW, DONATE_URL.toUri())) },
                // At most 55 % of the card, so a long label wraps in the button instead of squeezing the text.
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                LineIcon(Icons.Coffee, colors.onPrimary, size = 18.dp)
                Text(stringResource(R.string.settings_coffee), textAlign = TextAlign.Center, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    editing?.let { field ->
        val clockTime = field == CustomSetting.Reflection || field == CustomSetting.DayStart || field == CustomSetting.DayEnd
        CustomTimeDialog(
            title = stringResource(
                when (field) {
                    CustomSetting.Nudge -> R.string.settings_custom_nudge_title
                    CustomSetting.Resume -> R.string.settings_custom_resume_title
                    CustomSetting.Reflection -> R.string.settings_custom_reflection_title
                    CustomSetting.DayStart -> R.string.settings_custom_day_start_title
                    CustomSetting.DayEnd -> R.string.settings_custom_day_end_title
                },
            ),
            hint = stringResource(
                when (field) {
                    CustomSetting.Nudge -> R.string.settings_custom_nudge_hint
                    CustomSetting.Resume -> R.string.settings_custom_resume_hint
                    CustomSetting.Reflection -> R.string.settings_custom_time_hint
                    CustomSetting.DayStart, CustomSetting.DayEnd -> R.string.settings_custom_time_hint
                },
            ),
            error = stringResource(
                when (field) {
                    CustomSetting.Nudge -> R.string.settings_invalid_nudge
                    CustomSetting.Resume -> R.string.settings_invalid_resume
                    else -> R.string.settings_invalid_time
                },
            ),
            initial = when (field) {
                CustomSetting.Nudge -> settings.nudgeMinutes.toString()
                CustomSetting.Resume -> settings.resumeSeconds.toString()
                CustomSetting.Reflection -> formatMinuteOfDay(settings.reflectionMinute)
                CustomSetting.DayStart -> formatMinuteOfDay(settings.dayStartMinute)
                CustomSetting.DayEnd -> formatMinuteOfDay(settings.dayEndMinute)
            },
            clockTime = clockTime,
            validate = { text ->
                when (field) {
                    CustomSetting.Nudge -> text.trim().toIntOrNull()?.takeIf { it in 1..1440 }
                    CustomSetting.Resume -> text.trim().toIntOrNull()?.takeIf { it in 0..3600 }
                    else -> parseMinuteOfDay(text)
                }
            },
            onDismiss = { editing = null },
            onConfirm = { value ->
                scope.launch {
                    when (field) {
                        CustomSetting.Nudge -> repo.setNudgeMinutes(value)
                        CustomSetting.Resume -> repo.setResumeSeconds(value)
                        CustomSetting.Reflection -> repo.setReflectionMinute(value)
                        CustomSetting.DayStart -> repo.setDayStartMinute(value)
                        CustomSetting.DayEnd -> repo.setDayEndMinute(value)
                    }
                }
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onCustom: (() -> Unit)? = null,
    label: @Composable (Int) -> String,
) {
    val customSelected = onCustom != null && selected !in options
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            ChoiceChip(
                selected = option == selected,
                text = label(option),
                onClick = { onSelect(option) },
            )
        }
        if (onCustom != null) {
            ChoiceChip(
                selected = customSelected,
                text = if (customSelected) label(selected) else stringResource(R.string.settings_custom),
                onClick = onCustom,
            )
        }
    }
}

@Composable
private fun ChoiceChip(selected: Boolean, text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .height(44.dp)
            .background(if (selected) colors.primary else colors.surfaceContainerHigh, RoundedCornerShape(22.dp))
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) colors.onPrimary else colors.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun DayTimeRow(title: String, value: String, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onChange) { Text(stringResource(R.string.settings_change)) }
    }
}

@Composable
private fun CustomTimeDialog(
    title: String,
    hint: String,
    error: String,
    initial: String,
    clockTime: Boolean,
    validate: (String) -> Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                singleLine = true,
                isError = invalid,
                keyboardOptions = KeyboardOptions(keyboardType = if (clockTime) KeyboardType.Ascii else KeyboardType.Number),
                supportingText = { Text(if (invalid) error else hint) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = validate(text)
                    if (value == null) invalid = true else onConfirm(value)
                },
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { content() }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val light = Color(0xFFFBF6EE) to Color(0xFFF1E8DA)
    val dark = Color(0xFF17130F) to Color(0xFF2C251F)
    val (left, right) = when (mode) {
        ThemeMode.System -> light.first to dark.first
        ThemeMode.Light -> light
        ThemeMode.Dark -> dark
    }
    Column(
        modifier.selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .border(3.dp, if (selected) MaterialTheme.colorScheme.tertiary else Color.Transparent, RoundedCornerShape(22.dp))
                .padding(5.dp),
        ) {
            Box(Modifier.weight(1f).height(90.dp).background(left, RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp)), contentAlignment = Alignment.BottomCenter) {
                Pebble(PebbleStyle.Habit, 22.dp, Modifier.padding(bottom = 12.dp), color = Color(0xFFE8590C))
            }
            Box(Modifier.weight(1f).height(90.dp).background(right, RoundedCornerShape(topEnd = 17.dp, bottomEnd = 17.dp)), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.padding(top = 12.dp).size(20.dp).background(Color(0xFF7F95E8), RoundedCornerShape(7.dp)))
            }
        }
        Text(
            stringResource(
                when (mode) {
                    ThemeMode.System -> R.string.settings_theme_system
                    ThemeMode.Light -> R.string.settings_theme_light
                    ThemeMode.Dark -> R.string.settings_theme_dark
                },
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold),
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}