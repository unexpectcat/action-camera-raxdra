package app.actioncam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.actioncam.ui.common.Logo
import app.actioncam.ui.theme.Cam

/** Loading screen shown while the first scan starts. The logo is a placeholder (see Logo). */
@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize()) {
        // big yellow arc in the corner, the app's signature shape
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Cam.yellow, radius = size.width * 0.62f, center = Offset(size.width * 1.05f, -size.width * 0.08f))
            drawCircle(Cam.bg, radius = size.width * 0.44f, center = Offset(size.width * 1.05f, -size.width * 0.08f))
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(size = 46.dp)
            Spacer(Modifier.height(10.dp))
            Text("your pocket action camera", color = Cam.muted, fontSize = 14.sp)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(Modifier.width(120.dp), color = Cam.yellow, trackColor = Cam.card)
            Spacer(Modifier.height(12.dp))
            Text("Looking for cameras…", color = Cam.muted, fontSize = 12.sp)
        }
    }
}
