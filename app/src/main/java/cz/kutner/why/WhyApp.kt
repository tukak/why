package cz.kutner.why

import android.app.Application
import android.content.Context
import cz.kutner.why.service.EveningReflection
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
    }
}

val Context.container: AppContainer get() = (applicationContext as WhyApp).container
