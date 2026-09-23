package cz.kutner.why.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.settings.AppSettings
import cz.kutner.why.data.settings.ThemeMode
import cz.kutner.why.ui.timeFormatter
import cz.kutner.why.ui.components.BackgroundHelp
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import java.time.LocalTime
import kotlinx.coroutines.launch

/** Replace with the real page before release. */
private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.container.settings
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

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
            ChoiceChips(listOf(5, 10, 15, 0), selected = settings.nudgeMinutes, onSelect = { scope.launch { repo.setNudgeMinutes(it) } }) {
                if (it == 0) stringResource(R.string.settings_off) else stringResource(R.string.duration_minutes, it.toLong())
            }
            Column {
                Text(stringResource(R.string.settings_resume), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.settings_resume_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            ChoiceChips(listOf(0, 3, 10, 30), selected = settings.resumeSeconds, onSelect = { scope.launch { repo.setResumeSeconds(it) } }) {
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
                ChoiceChips(listOf(20, 21, 22, 23).map { it * 60 }, selected = settings.reflectionMinute, onSelect = { scope.launch { repo.setReflectionMinute(it) } }) {
                    LocalTime.of(it / 60, it % 60).format(timeFormat)
                }
            }
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
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_free_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_free_text), style = MaterialTheme.typography.bodySmall, color = colors.onTertiaryContainer)
            }
            Button(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, BUY_ME_A_COFFEE_URL.toUri()))
                } catch (_: ActivityNotFoundException) {
                }
            }) {
                LineIcon(Icons.Coffee, colors.onPrimary, size = 18.dp)
                Text(stringResource(R.string.settings_coffee), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ChoiceChips(options: List<Int>, selected: Int, onSelect: (Int) -> Unit, label: @Composable (Int) -> String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (isSelected) colors.primary else colors.surfaceContainerHigh, RoundedCornerShape(22.dp))
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
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

