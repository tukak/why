package cz.kutner.why

import android.app.Application
import android.content.Context
import kotlinx.coroutines.launch

class WhyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch { container.unlocks.seedIfEmpty(container::defaultReasons) }
    }
}

val Context.container: AppContainer get() = (applicationContext as WhyApp).container
