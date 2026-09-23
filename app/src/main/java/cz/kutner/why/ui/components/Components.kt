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
    const val Gear =
        "M19.39 10.23 L21.91 10.67 L21.91 13.33 L19.39 13.77 L18.48 15.97 L19.95 18.07 L18.07 19.95 L15.97 18.48 " +
            "L13.77 19.39 L13.33 21.91 L10.67 21.91 L10.23 19.39 L8.03 18.48 L5.93 19.95 L4.05 18.07 L5.52 15.97 " +
            "L4.61 13.77 L2.09 13.33 L2.09 10.67 L4.61 10.23 L5.52 8.03 L4.05 5.93 L5.93 4.05 L8.03 5.52 L10.23 4.61 " +
            "L10.67 2.09 L13.33 2.09 L13.77 4.61 L15.97 5.52 L18.07 4.05 L19.95 5.93 L18.48 8.03Z M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6z"
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
