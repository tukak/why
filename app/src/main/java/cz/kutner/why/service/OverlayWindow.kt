package cz.kutner.why.service

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** A full-screen window above other apps that hosts Compose content. */
private class OverlayWindow(
    private val context: Context,
    private val content: @Composable () -> Unit,
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var view: ComposeView? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun show(): Boolean {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayWindow)
            setViewTreeViewModelStoreOwner(this@OverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@OverlayWindow)
            setContent(content)
        }
        return try {
            windowManager.addView(composeView, layoutParams())
            view = composeView
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cannot add overlay", e)
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            false
        }
    }

    fun close() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        view?.let { runCatching { windowManager.removeViewImmediate(it) } }
        view = null
        viewModelStore.clear()
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Overlays cannot set status bar icon colors, so the status bar stays with the app underneath.
        fitInsetsTypes = WindowInsets.Type.statusBars()
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
    }
}

/** Shows at most one overlay at a time. Call from the main thread. */
class OverlayController(private val context: Context) {
    private var current: OverlayWindow? = null

    fun canShow(): Boolean = Settings.canDrawOverlays(context)

    fun show(content: @Composable () -> Unit) {
        dismiss()
        if (!canShow()) return
        val window = OverlayWindow(context, content)
        if (window.show()) current = window
    }

    fun dismiss() {
        current?.close()
        current = null
    }
}

private const val TAG = "OverlayWindow"
