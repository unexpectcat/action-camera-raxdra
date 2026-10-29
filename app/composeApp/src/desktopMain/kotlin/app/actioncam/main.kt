package app.actioncam

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Desktop build: a phone-sized window, handy for trying the app against a camera on the laptop. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Action Cam", state = rememberWindowState(width = 400.dp, height = 820.dp)) {
        App()
    }
}
