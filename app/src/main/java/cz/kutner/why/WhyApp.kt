package cz.kutner.why

import android.app.Application
import android.content.Context
import android.provider.Settings
import cz.kutner.why.service.EveningReflection
import cz.kutner.why.service.UnlockService
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class WhyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch { container.unlocks.seedIfEmpty(container::defaultReasons) }
        // Alarms are lost on reboot and app update; any of those starts this process.
        container.applicationScope.launch {
            container.settings.settings.distinctUntilChangedBy { it.reflectionEnabled to it.reflectionMinute }
                .collect { EveningReflection.schedule(this@WhyApp, it) }
        }
        // The system can stop the app without restarting its service; it later starts the bare process, for example to preload it.
        container.applicationScope.launch {
            if (container.settings.current().onboardingDone && Settings.canDrawOverlays(this@WhyApp)) {
                UnlockService.ensureRunning(this@WhyApp)
            }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as WhyApp).container
