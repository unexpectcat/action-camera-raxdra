package app.actioncam.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// Things that differ between Android and desktop.

/** Read a file from the app's private data folder, or null if missing. */
expect fun readAppFile(name: String): String?

/** Atomically write a file in the app's private data folder. */
expect fun writeAppFile(name: String, text: String)

expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** IPv4 addresses of this device's network interfaces (Wi-Fi, hotspot), without loopback. */
expect fun localIpv4Addresses(): List<String>

/** A video being saved to the phone. */
interface DownloadSink {
    fun write(bytes: ByteArray, length: Int)
    /** Makes the file visible (gallery) and returns the reference to open it later. */
    fun finish(): String
    fun abort()
}

/** Starts saving a video to the phone's Movies/ActionCam folder. */
expect fun createDownload(fileName: String): DownloadSink

/** False when a downloaded video was deleted from the phone since. */
expect fun downloadExists(ref: String): Boolean

/** Opens a downloaded video in the phone's video player. */
expect fun openVideo(ref: String)

/** System back button / gesture. */
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)
