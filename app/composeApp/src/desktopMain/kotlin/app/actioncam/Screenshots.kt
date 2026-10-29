package app.actioncam

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import app.actioncam.model.CamInfo
import app.actioncam.model.Device
import app.actioncam.model.Video
import app.actioncam.platform.decodeImage
import kotlinx.coroutines.MainScope
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Rect
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

/**
 * Renders every screen with sample data, headless, at phone size:
 * ./gradlew :composeApp:screenshots  ->  PNGs in composeApp/build/screenshots
 */
fun main(args: Array<String>) {
    val out = File(args.getOrNull(0) ?: "build/screenshots").apply { mkdirs() }
    System.setProperty("actioncam.dataDir", Files.createTempDirectory("actioncam-shots").toString())

    val cam = Device("192.168.1.131", name = "action-cam", online = true)
    val cam2 = Device("192.168.43.17", name = "helmet-cam", online = true, recording = true)
    val old = Device("192.168.1.140", name = "bike-cam", manual = true)
    val videos = listOf(
        Video("2026-10-29_12-30-05.mp4", 412_000_000, recording = true),
        Video("2026-10-29_11-02-44.mp4", 187_400_000),
        Video("2026-10-29_10-15-09.mp4", 96_100_000),
        Video("2026-10-28_18-47-31.mp4", 1_250_000_000),
    )

    fun shot(name: String, setup: (AppState) -> Unit) {
        val state = AppState(MainScope())
        setup(state)
        val scene = ImageComposeScene(1080, 2340, Density(2.75f)) { AppContent(state) }
        repeat(4) { scene.render(it * 500_000_000L) }
        val img = scene.render(4_000_000_000L)
        File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
        println("wrote $name.png")
    }

    shot("1-splash") { it.screen = Screen.Splash }
    shot("2-devices") { it.devices = listOf(cam, cam2, old); it.screen = Screen.Devices }
    shot("3-devices-empty") { it.devices = emptyList(); it.screen = Screen.Devices }
    shot("4-camera") {
        it.screen = Screen.Camera(cam); it.info = CamInfo("action-cam", "camera"); it.photo = samplePhoto()
    }
    shot("5-camera-recording") {
        it.screen = Screen.Camera(cam2)
        it.info = CamInfo("helmet-cam", "camera", recording = true, recordingName = "2026-10-29_12-30-05.mp4", recordingSeconds = 135)
        it.photo = samplePhoto(); it.toast = "Recording started"
    }
    shot("6-camera-empty") { it.screen = Screen.Camera(cam); it.info = CamInfo("action-cam", "test") }
    shot("7-library") {
        it.screen = Screen.Library(cam); it.videos = videos
        it.progress[it.videoKey(cam, videos[2])] = 0.42f
        it.downloaded[it.videoKey(cam, videos[1])] = "/tmp/x.mp4"
    }
    shot("8-library-empty") { it.screen = Screen.Library(cam); it.videos = emptyList() }
    exitProcess(0)
}

/** A gradient stand-in for a camera photo. */
private fun samplePhoto() = Surface.makeRasterN32Premul(1280, 720).run {
    canvas.drawRect(Rect(0f, 0f, 1280f, 720f), Paint().apply {
        shader = Shader.makeLinearGradient(0f, 0f, 1280f, 720f, intArrayOf(0xFF3A6EA5.toInt(), 0xFFE8A33D.toInt(), 0xFF2F4F3A.toInt()))
    })
    decodeImage(makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG)!!.bytes)
}
