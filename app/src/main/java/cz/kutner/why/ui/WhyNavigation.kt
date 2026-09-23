package cz.kutner.why.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cz.kutner.why.R
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.reasons.ReasonsScreen
import cz.kutner.why.ui.settings.SettingsScreen
import cz.kutner.why.ui.today.TodayScreen
import cz.kutner.why.ui.week.WeekScreen
import kotlinx.serialization.Serializable

@Serializable data object TodayKey : NavKey
@Serializable data object WeekKey : NavKey
@Serializable data object ReasonsKey : NavKey
@Serializable data object SettingsKey : NavKey

private data class Tab(val key: NavKey, val label: Int, val icon: String)

private val tabs = listOf(
    Tab(TodayKey, R.string.tab_today, Icons.Jar),
    Tab(WeekKey, R.string.tab_week, Icons.Bars),
    Tab(ReasonsKey, R.string.tab_reasons, Icons.Shapes),
)

@Composable
fun WhyNavigation() {
    val backStack = rememberNavBackStack(TodayKey)
    val top = backStack.lastOrNull()
    Scaffold(
        bottomBar = { if (tabs.any { it.key == top }) WhyNavBar(top) { backStack.selectTab(it) } },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayKey> { TodayScreen(onSettings = { backStack.add(SettingsKey) }) }
                entry<WeekKey> { WeekScreen() }
                entry<ReasonsKey> { ReasonsScreen() }
                entry<SettingsKey> { SettingsScreen(onBack = { backStack.removeLastOrNull() }) }
            },
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        )
    }
}

/** Today is the root; other tabs sit one level above it, so Back returns to Today. */
private fun NavBackStack<NavKey>.selectTab(key: NavKey) {
    while (size > 1) removeAt(lastIndex)
    if (key != TodayKey) add(key)
}

@Composable
private fun WhyNavBar(current: NavKey?, onSelect: (NavKey) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        tabs.forEach { tab ->
            val selected = tab.key == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.key) },
                icon = { LineIcon(tab.icon, if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) },
                label = { Text(stringResource(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
