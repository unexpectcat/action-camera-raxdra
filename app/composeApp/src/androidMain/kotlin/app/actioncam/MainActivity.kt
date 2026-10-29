package app.actioncam

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.actioncam.platform.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // dark app: light status/navigation bar icons
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

/** Sets up the platform layer before anything else runs. */
class ActionCamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.context = applicationContext
    }
}
