package app.actioncam.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val home = System.getProperty("user.home")

/** ~/.local/share/action-cam, or -Dactioncam.dataDir=… (used by the screenshot tool). */
private val dataDir: File by lazy {
    val dir = System.getProperty("actioncam.dataDir")?.let(::File)
        ?: File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "action-cam")
    dir.apply { mkdirs() }
}

actual fun readAppFile(name: String): String? =
    File(dataDir, name).takeIf { it.exists() }?.readText()

actual fun writeAppFile(name: String, text: String) {
    val tmp = File(dataDir, "$name.tmp")
    tmp.writeText(text)
    Files.move(tmp.toPath(), File(dataDir, name).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

actual fun localIpv4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.name.startsWith("virbr") && !it.name.startsWith("docker") }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress }
}.getOrDefault(emptyList())

private class FileSink(private val file: File) : DownloadSink {
    private val part = File(file.path + ".part")
    private val out = part.outputStream()
    override fun write(bytes: ByteArray, length: Int) = out.write(bytes, 0, length)
    override fun finish(): String { out.close(); part.renameTo(file); return file.absolutePath }
    override fun abort() { runCatching { out.close() }; part.delete() }
}

/** ~/Videos/ActionCam */
actual fun createDownload(fileName: String): DownloadSink =
    FileSink(File(File(home, "Videos/ActionCam").apply { mkdirs() }, fileName))

actual fun downloadExists(ref: String): Boolean = File(ref).exists()

actual fun openVideo(ref: String) {
    runCatching { Desktop.getDesktop().open(File(ref)) }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // desktop: the in-app back buttons are enough
}
