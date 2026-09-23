package cz.kutner.why

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cz.kutner.why.data.settings.ThemeMode
import cz.kutner.why.service.UnlockService
import cz.kutner.why.ui.WhyNavigation
import cz.kutner.why.ui.onboarding.OnboardingScreen
import cz.kutner.why.ui.theme.WhyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var ready = false
        splash.setKeepOnScreenCondition { !ready }

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            LifecycleResumeEffect(Unit) {
                overlayGranted = Settings.canDrawOverlays(this@MainActivity)
                onPauseOrDispose { }
            }
            settings?.let { current ->
                ready = true
                val dark = when (current.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
                LaunchedEffect(dark) {
                    val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                WhyTheme(current.themeMode, current.dynamicColor) {
                    if (current.onboardingDone && overlayGranted) {
                        WhyNavigation()
                    } else {
                        OnboardingScreen(
                            permissionLost = current.onboardingDone,
                            onDone = {
                                scope.launch {
                                    container.settings.setOnboardingDone()
                                    UnlockService.start(this@MainActivity)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (container.settings.current().onboardingDone && Settings.canDrawOverlays(this@MainActivity)) {
                UnlockService.start(this@MainActivity)
            }
        }
    }
}
