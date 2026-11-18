package io.github.kadaferhidup.minibank.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palet yang sama dengan versi web MiniBank (src/main/resources/static/css/app.css). */
object MB {
    val Bg = Color(0xFFBFBDB8)
    val Card = Color(0xFFDAD7D2)
    val Card2 = Color(0xFFE7E4E0)
    val Paper = Color(0xFFF1EFEB)
    val Ink = Color(0xFF0B0B0B)
    val Ink2 = Color(0xFF4A4844) // teks sekunder, kontras > 4.5 di atas Bg
    val Ink3 = Color(0xFF66645F)
    val Line = Color(0x240B0B0B)
    val Line2 = Color(0x140B0B0B)
    val Red = Color(0xFFE42320)
    val RedInk = Color(0xFFB3150F) // nominal keluar
    val Green = Color(0xFF1E6B3A) // nominal masuk, gelap supaya tetap terbaca
}

private val colors = lightColorScheme(
    primary = MB.Ink,
    onPrimary = Color.White,
    secondary = MB.Red,
    onSecondary = Color.White,
    background = MB.Bg,
    onBackground = MB.Ink,
    surface = MB.Card,
    onSurface = MB.Ink,
    surfaceVariant = MB.Card2,
    onSurfaceVariant = MB.Ink2,
    surfaceContainer = MB.Card,
    surfaceContainerHigh = MB.Card2,
    surfaceContainerHighest = MB.Paper,
    outline = MB.Ink3,
    outlineVariant = MB.Line,
    error = MB.RedInk,
    onError = Color.White,
)

private val type = Typography(
    displaySmall = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.4).sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

private val shapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MiniBankTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, shapes = shapes, content = content)
}
