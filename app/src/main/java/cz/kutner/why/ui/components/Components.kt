package cz.kutner.why.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.kutner.why.ui.theme.PebbleStyle

@Composable
fun Pebble(style: PebbleStyle, size: Dp, modifier: Modifier = Modifier, color: Color = style.color.tones.ink) {
    Box(modifier.size(size).background(color, style.shape.shape))
}

/** Line icon drawn from a 24×24 SVG path. */
@Composable
fun LineIcon(pathData: String, color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, strokeWidth: Float = 2f) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

object Icons {
    const val ArrowRight = "M5 12h14 M13 6l6 6-6 6"
    const val ArrowLeft = "M19 12H5 M11 6l-6 6 6 6"
    const val ArrowDown = "M12 5v14 M6 13l6 6 6-6"
    const val ArrowUp = "M12 19V5 M6 11l6-6 6 6"
    const val Check = "M5 12l5 5L20 7"
    const val Layers = "M12 3l9 5-9 5-9-5 9-5z M3 13l9 5 9-5"
    const val Battery = "M8 6h8a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z M10 3h4 M9 14l2 2 4-4"
    const val Bell = "M6 16v-5a6 6 0 0 1 12 0v5l2 2H4l2-2z M10 21h4"
    const val Lock = "M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z M8 11V8a4 4 0 0 1 8 0v3"
    const val Jar = "M10 5h4a5 5 0 0 1 5 5v6a5 5 0 0 1-5 5h-4a5 5 0 0 1-5-5v-6a5 5 0 0 1 5-5z M8 3h8"
    const val Bars = "M6 20V11 M12 20V5 M18 20v-6"
    const val Shapes = "M7.5 4a3.5 3.5 0 1 1 0 7a3.5 3.5 0 1 1 0-7z M15.5 4h2a2.5 2.5 0 0 1 2.5 2.5v2a2.5 2.5 0 0 1-2.5 2.5h-2a2.5 2.5 0 0 1-2.5-2.5v-2a2.5 2.5 0 0 1 2.5-2.5z M7.5 13.5l3.5 6H4z M16.5 13a3.5 3.5 0 1 1 0 7a3.5 3.5 0 1 1 0-7z"
    const val Gear = "M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z M12 2v3 M12 19v3 M4.9 4.9l2.1 2.1 M17 17l2.1 2.1 M2 12h3 M19 12h3 M4.9 19.1L7 17 M17 7l2.1-2.1"
    const val Pencil = "M4 20h4L19 9l-4-4L4 16v4z"
    const val Plus = "M12 5v14 M5 12h14"
    const val Chevron = "M9 6l6 6-6 6"
    const val Coffee = "M4 9h13v5a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V9z M17 11h1.5a2.5 2.5 0 0 1 0 5H17 M8 3v2 M12 3v2"
}

/** Text that is either a user-given string or a resource. */
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Res(val id: Int) : UiText

    @Composable
    fun resolve(): String = when (this) {
        is Raw -> value
        is Res -> stringResource(id)
    }
}
