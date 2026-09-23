package cz.kutner.why.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import cz.kutner.why.R
import cz.kutner.why.ui.components.BackgroundHelp
import cz.kutner.why.ui.components.Icons
import cz.kutner.why.ui.components.LineIcon
import cz.kutner.why.ui.components.Pebble
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(permissionLost: Boolean = false, onDone: () -> Unit) {
    val context = LocalContext.current
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationsGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var unrestricted by remember { mutableStateOf(BackgroundHelp.isUnrestricted(context)) }
    LifecycleResumeEffect(Unit) {
        overlayGranted = Settings.canDrawOverlays(context)
        notificationsGranted = notificationsGranted(context)
        unrestricted = BackgroundHelp.isUnrestricted(context)
        onPauseOrDispose { }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsGranted = it }
    val colors = MaterialTheme.colorScheme

    Surface(color = colors.background, contentColor = colors.onBackground) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ShapePile()
            if (permissionLost && !overlayGranted) {
                val tones = ReasonColor.Ember.tones
                Text(
                    stringResource(R.string.onboarding_permission_lost),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth().background(tones.container, RoundedCornerShape(20.dp)).padding(16.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.displaySmall.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.1f))
                Text(stringResource(R.string.onboarding_text), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }

            Column(Modifier.background(colors.surfaceContainerLowest, RoundedCornerShape(28.dp)).padding(vertical = 6.dp)) {
                PermissionRow(Icons.Check, ReasonColor.Green, R.string.onboarding_unlocks, R.string.onboarding_unlocks_hint, granted = true, onAllow = {})
                PermissionRow(Icons.Layers, ReasonColor.Ember, R.string.onboarding_overlay, R.string.onboarding_overlay_hint, granted = overlayGranted) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                }
                PermissionRow(Icons.Bell, ReasonColor.Blue, R.string.onboarding_notification, R.string.onboarding_notification_hint, granted = notificationsGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                PermissionRow(Icons.Battery, ReasonColor.Violet, R.string.onboarding_battery, R.string.onboarding_battery_hint, granted = unrestricted) {
                    BackgroundHelp.openAppSettings(context)
                }
                TextButton(onClick = { BackgroundHelp.openManufacturerTips(context) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.background_tips, BackgroundHelp.manufacturer))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineIcon(Icons.Lock, colors.onSurfaceVariant, size = 18.dp)
                Text(stringResource(R.string.onboarding_privacy), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }

            Button(onClick = onDone, enabled = overlayGranted, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.labelLarge)
            }
            if (!overlayGranted) {
                Text(stringResource(R.string.onboarding_needs_overlay), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionRow(icon: String, tone: ReasonColor, title: Int, hint: Int, granted: Boolean, onAllow: () -> Unit) {
    val tones = tone.tones
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).background(tones.container, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            LineIcon(icon, tones.ink, strokeWidth = 2.2f)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            val green = ReasonColor.Green.tones
            Text(
                stringResource(R.string.onboarding_ready),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = green.ink,
                modifier = Modifier.background(green.container, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            FilledTonalButton(onClick = onAllow) { Text(stringResource(R.string.onboarding_allow)) }
        }
    }
}

private data class PileItem(val style: PebbleStyle, val size: Dp, val x: Dp, val y: Dp, val rotation: Float)

private val pile = listOf(
    PileItem(PebbleStyle(PebbleShape.Circle, ReasonColor.Amber), 44.dp, 26.dp, 4.dp, 0f),
    PileItem(PebbleStyle(PebbleShape.Squircle, ReasonColor.Blue), 82.dp, 84.dp, 0.dp, 9f),
    PileItem(PebbleStyle(PebbleShape.Flower, ReasonColor.Pink), 88.dp, 218.dp, 2.dp, 14f),
    PileItem(PebbleStyle.Habit, 96.dp, 0.dp, 50.dp, -12f),
    PileItem(PebbleStyle(PebbleShape.Triangle, ReasonColor.Teal), 62.dp, 102.dp, 86.dp, 10f),
    PileItem(PebbleStyle(PebbleShape.Clover, ReasonColor.Violet), 74.dp, 158.dp, 72.dp, -6f),
    PileItem(PebbleStyle(PebbleShape.Pill, ReasonColor.Green), 74.dp, 264.dp, 90.dp, -22f),
)

@Composable
private fun ShapePile() {
    Box(Modifier.fillMaxWidth().height(170.dp)) {
        pile.forEachIndexed { i, item ->
            val drop = remember { Animatable(-160f) }
            LaunchedEffect(Unit) {
                delay(i * 90L)
                drop.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 220f))
            }
            Pebble(
                item.style,
                item.size,
                Modifier
                    .offset(item.x, item.y)
                    .graphicsLayer {
                        translationY = drop.value * density
                        rotationZ = item.rotation
                    },
            )
        }
    }
}
