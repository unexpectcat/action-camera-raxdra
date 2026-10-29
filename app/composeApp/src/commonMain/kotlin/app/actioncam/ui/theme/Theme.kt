package app.actioncam.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Charcoal screens, raised dark cards, one strong yellow for actions.

object Cam {
    val bg = Color(0xFF232529)
    val card = Color(0xFF2E3136)
    val cardHigh = Color(0xFF383C42)
    val deep = Color(0xFF1A1C1F)
    val yellow = Color(0xFFFFC20E)
    val onYellow = Color(0xFF1A1C1F)
    val fg = Color(0xFFF4F4F4)
    val muted = Color(0xFF9097A0)
    val line = Color(0x1FFFFFFF)
    val red = Color(0xFFFF4D4D)
    val green = Color(0xFF4CD787)
}

@Composable
fun CamTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Cam.yellow, onPrimary = Cam.onYellow,
        background = Cam.bg, onBackground = Cam.fg,
        surface = Cam.card, onSurface = Cam.fg, onSurfaceVariant = Cam.muted,
        surfaceContainerHigh = Cam.card, surfaceContainerHighest = Cam.cardHigh,
        outline = Cam.muted, outlineVariant = Cam.line, error = Cam.red,
    )
    val base = Typography()
    val type = base.copy(
        headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
        titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        bodyMedium = TextStyle(fontSize = 14.sp),
        bodySmall = TextStyle(fontSize = 12.sp),
        labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
    )
    val shapes = Shapes(
        small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
