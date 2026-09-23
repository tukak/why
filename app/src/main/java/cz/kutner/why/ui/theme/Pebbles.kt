package cz.kutner.why.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Scales a path drawn on a 24×24 grid to any size. */
class GridPathShape(pathData: String) : Shape {
    private val source: Path = PathParser().parsePathString(pathData).toPath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            addPath(source)
            transform(Matrix().apply { scale(size.width / 24f, size.height / 24f) })
        }
        return Outline.Generic(path)
    }
}

enum class PebbleShape(pathData: String) {
    Squircle("M9 2h6c4 0 7 3 7 7v6c0 4-3 7-7 7H9c-4 0-7-3-7-7V9c0-4 3-7 7-7z"),
    Clover(
        "M8 2.4a5.6 5.6 0 1 1 0 11.2a5.6 5.6 0 1 1 0-11.2z M16 2.4a5.6 5.6 0 1 1 0 11.2a5.6 5.6 0 1 1 0-11.2z " +
            "M8 10.4a5.6 5.6 0 1 1 0 11.2a5.6 5.6 0 1 1 0-11.2z M16 10.4a5.6 5.6 0 1 1 0 11.2a5.6 5.6 0 1 1 0-11.2z",
    ),
    Pill("M8 6h8a6 6 0 0 1 0 12H8a6 6 0 0 1 0-12z"),
    Flower(
        "M18.2 7.4a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z M15.1 12.77a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z " +
            "M8.9 12.77a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z M5.8 7.4a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z " +
            "M8.9 2.03a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z M15.1 2.03a4.6 4.6 0 1 1 0 9.2a4.6 4.6 0 1 1 0-9.2z " +
            "M12 7a5 5 0 1 1 0 10a5 5 0 1 1 0-10z",
    ),
    Triangle("M10.27 4a2 2 0 0 1 3.46 0l7.8 13.5a2 2 0 0 1-1.73 3H4.2a2 2 0 0 1-1.73-3z"),
    Circle("M12 2a10 10 0 1 1 0 20a10 10 0 1 1 0-20z"),
    Diamond("M10.6 2.6a2 2 0 0 1 2.8 0l8 8a2 2 0 0 1 0 2.8l-8 8a2 2 0 0 1-2.8 0l-8-8a2 2 0 0 1 0-2.8z"),
    Burst(
        "M12 1L14.17 3.89L17.5 2.47L17.94 6.06L21.53 6.5L20.11 9.83L23 12L20.11 14.17L21.53 17.5L17.94 17.94" +
            "L17.5 21.53L14.17 20.11L12 23L9.83 20.11L6.5 21.53L6.06 17.94L2.47 17.5L3.89 14.17L1 12L3.89 9.83" +
            "L2.47 6.5L6.06 6.06L6.5 2.47L9.83 3.89Z",
    );

    val shape: Shape = GridPathShape(pathData)

    companion object {
        /** Shapes a user can pick. [Burst] belongs to "Just habit". */
        val pickable = entries - Burst
        fun of(key: String): PebbleShape = entries.firstOrNull { it.name == key } ?: Circle
    }
}

data class ReasonTones(val ink: Color, val container: Color)

enum class ReasonColor(private val light: ReasonTones, private val dark: ReasonTones) {
    Blue(ReasonTones(Color(0xFF3451C7), Color(0xFFDDE5FF)), ReasonTones(Color(0xFFA9BCFF), Color(0xFF25305A))),
    Violet(ReasonTones(Color(0xFF6741D9), Color(0xFFECE3FF)), ReasonTones(Color(0xFFC9B6FF), Color(0xFF33275A))),
    Green(ReasonTones(Color(0xFF2B8A3E), Color(0xFFD6F3DE)), ReasonTones(Color(0xFF8FD9A5), Color(0xFF1E3A28))),
    Pink(ReasonTones(Color(0xFFC2255C), Color(0xFFFFE0EA)), ReasonTones(Color(0xFFFFA8C5), Color(0xFF4A2030))),
    Teal(ReasonTones(Color(0xFF0B7285), Color(0xFFD0F0F3)), ReasonTones(Color(0xFF7FD6E0), Color(0xFF133A40))),
    Amber(ReasonTones(Color(0xFFB35C00), Color(0xFFFFEBC7)), ReasonTones(Color(0xFFF5C35A), Color(0xFF3F2E10))),
    Ember(ReasonTones(Color(0xFFE8590C), Color(0xFFFFE1CF)), ReasonTones(Color(0xFFFF9A5C), Color(0xFF4A2410))),
    Stone(ReasonTones(Color(0xFFA89C8B), Color(0xFFEFE7DA)), ReasonTones(Color(0xFF6E6358), Color(0xFF2C251F)));

    val tones: ReasonTones
        @Composable @ReadOnlyComposable get() = if (LocalIsDark.current) dark else light

    companion object {
        /** Colors a user can pick. [Ember] belongs to "Just habit"; [Stone] marks other answers. */
        val pickable = entries - Ember - Stone
        fun of(key: String): ReasonColor = entries.firstOrNull { it.name == key } ?: Stone
    }
}

data class PebbleStyle(val shape: PebbleShape, val color: ReasonColor) {
    companion object {
        val Habit = PebbleStyle(PebbleShape.Burst, ReasonColor.Ember)
        val Other = PebbleStyle(PebbleShape.Diamond, ReasonColor.Stone)
        val Unanswered = PebbleStyle(PebbleShape.Circle, ReasonColor.Stone)
    }
}
