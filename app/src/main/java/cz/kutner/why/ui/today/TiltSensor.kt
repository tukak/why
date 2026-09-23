package cz.kutner.why.ui.today

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.abs

/**
 * The acceleration a loose object feels, in screen coordinates (x right, y down), m/s².
 * Includes shaking. Without a sensor it stays at plain gravity.
 */
class TiltSensor(private val onChange: () -> Unit) : SensorEventListener {
    var ax = 0f
        private set
    var ay = SensorManager.GRAVITY_EARTH
        private set
    var rotation = Surface.ROTATION_0

    private var reportedX = ax
    private var reportedY = ay

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val (sx, sy) = when (rotation) {
            Surface.ROTATION_90 -> y to x
            Surface.ROTATION_180 -> x to -y
            Surface.ROTATION_270 -> -y to -x
            else -> -x to y
        }
        ax = sx
        ay = sy
        if (abs(sx - reportedX) + abs(sy - reportedY) > WAKE_DELTA) {
            reportedX = sx
            reportedY = sy
            onChange()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val WAKE_DELTA = 0.6f
    }
}

/** Listens only while the screen is resumed, so a closed jar costs no battery. */
@Composable
fun rememberTiltSensor(onChange: () -> Unit): TiltSensor {
    val context = LocalContext.current
    val view = LocalView.current
    val tilt = remember { TiltSensor(onChange) }
    LifecycleResumeEffect(tilt) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tilt.rotation = view.display?.rotation ?: Surface.ROTATION_0
        if (sensor != null) manager.registerListener(tilt, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { manager.unregisterListener(tilt) }
    }
    return tilt
}
