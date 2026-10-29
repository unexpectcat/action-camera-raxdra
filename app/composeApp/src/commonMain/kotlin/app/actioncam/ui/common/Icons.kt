package app.actioncam.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Small line icons on a 24 grid, so the app doesn't pull in the large icon library.

private fun icon(name: String, fill: Boolean = false, block: PathBuilder.() -> Unit) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        if (fill) path(fill = SolidColor(Color.Black), pathBuilder = block)
        else path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block,
        )
    }.build()

object CamIcons {
    val Plus = icon("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    val Back = icon("back") { moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f) }
    val Close = icon("close") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) }
    val Download = icon("download") { moveTo(12f, 5f); lineTo(12f, 16f); moveTo(7f, 11.5f); lineTo(12f, 16.5f); lineTo(17f, 11.5f); moveTo(6f, 19.5f); lineTo(18f, 19.5f) }
    val Play = icon("play", fill = true) { moveTo(8.5f, 5.5f); lineTo(19f, 12f); lineTo(8.5f, 18.5f); close() }
    val Stop = icon("stop", fill = true) { moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close() }
    val Camera = icon("camera") {
        moveTo(4f, 8f); lineTo(4f, 18f); lineTo(20f, 18f); lineTo(20f, 8f); lineTo(16f, 8f)
        lineTo(14.5f, 5.5f); lineTo(9.5f, 5.5f); lineTo(8f, 8f); close()
        moveTo(15.2f, 12.8f); arcTo(3.2f, 3.2f, 0f, true, true, 8.8f, 12.8f); arcTo(3.2f, 3.2f, 0f, true, true, 15.2f, 12.8f)
    }
    val Film = icon("film") {
        moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(9f, 4f); lineTo(9f, 20f); moveTo(15f, 4f); lineTo(15f, 20f)
        moveTo(5f, 9f); lineTo(9f, 9f); moveTo(5f, 15f); lineTo(9f, 15f); moveTo(15f, 9f); lineTo(19f, 9f); moveTo(15f, 15f); lineTo(19f, 15f)
    }
    val Image = icon("image") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(4f, 16f); lineTo(9f, 11f); lineTo(13f, 15f); lineTo(15.5f, 12.5f); lineTo(20f, 17f)
    }
}
