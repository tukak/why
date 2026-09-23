package cz.kutner.why.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cz.kutner.why.R

private fun bricolage(weight: Int) = Font(
    R.font.bricolage_grotesque,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 96f)),
)

private fun figtree(weight: Int) = Font(
    R.font.figtree,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Bricolage = FontFamily(bricolage(600), bricolage(700), bricolage(800))
val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700), figtree(800))

private val display = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em)
private val body = TextStyle(fontFamily = Figtree)

val BloomTypography = Typography(
    displayLarge = display.copy(fontSize = 76.sp, lineHeight = 70.sp, letterSpacing = (-0.04).em),
    displayMedium = display.copy(fontSize = 54.sp, lineHeight = 54.sp),
    displaySmall = display.copy(fontSize = 46.sp, lineHeight = 46.sp),
    headlineLarge = display.copy(fontSize = 40.sp, lineHeight = 42.sp),
    headlineMedium = display.copy(fontSize = 34.sp, lineHeight = 38.sp),
    headlineSmall = display.copy(fontSize = 28.sp, lineHeight = 32.sp),
    titleLarge = display.copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
    titleMedium = display.copy(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.em),
    titleSmall = body.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = body.copy(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = body.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = body.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = body.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
)
