package app.actioncam

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import app.actioncam.platform.BackHandler
import app.actioncam.ui.CameraScreen
import app.actioncam.ui.DevicesScreen
import app.actioncam.ui.LibraryScreen
import app.actioncam.ui.SplashScreen
import app.actioncam.ui.common.Toast
import app.actioncam.ui.theme.Cam
import app.actioncam.ui.theme.CamTheme
import kotlinx.coroutines.delay

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }
    LaunchedEffect(Unit) {
        state.scan()
        delay(1_600)
        state.screen = Screen.Devices
    }
    AppContent(state)
}

@Composable
fun AppContent(state: AppState) = CamTheme {
    BackHandler(state.screen is Screen.Camera || state.screen is Screen.Library) { state.back() }
    Box(Modifier.fillMaxSize().background(Cam.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        AnimatedContent(state.screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it::class }) { screen ->
            when (screen) {
                Screen.Splash -> SplashScreen()
                Screen.Devices -> DevicesScreen(
                    devices = state.devices, scanning = state.scanning, onRefresh = state::scan,
                    onOpen = state::open, onLibrary = state::openLibrary, onAdd = state::addDevice,
                )
                is Screen.Camera -> {
                    LaunchedEffect(screen.device.key) { state.pollInfo(screen.device) }
                    CameraScreen(
                        device = screen.device, info = state.info, photo = state.photo,
                        photoLoading = state.photoLoading, takingPhoto = state.takingPhoto, recordBusy = state.recordBusy,
                        onBack = state::back, onPhoto = { state.takePhoto(screen.device) },
                        onRecord = { state.toggleRecording(screen.device) }, onLibrary = { state.openLibrary(screen.device) },
                    )
                }
                is Screen.Library -> LibraryScreen(
                    device = screen.device, videos = state.videos, loading = state.libraryLoading, error = state.libraryError,
                    progressOf = { state.progress[state.videoKey(screen.device, it)] },
                    isDownloaded = { state.isDownloaded(screen.device, it) },
                    onBack = state::back, onRefresh = { state.loadVideos(screen.device) },
                    onTap = { state.tapVideo(screen.device, it) },
                )
            }
        }
        Toast(state.toast) { state.toast = null }
    }
}
