package app.actioncam.platform

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface

@SuppressLint("StaticFieldLeak") // application context only
object AndroidPlatform {
    lateinit var context: Context
}

private val ctx get() = AndroidPlatform.context

actual fun readAppFile(name: String): String? =
    File(ctx.filesDir, name).takeIf { it.exists() }?.readText()

actual fun writeAppFile(name: String, text: String) {
    val tmp = File(ctx.filesDir, "$name.tmp")
    tmp.writeText(text)
    tmp.renameTo(File(ctx.filesDir, name))
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

actual fun localIpv4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress.orEmpty() }
}.getOrDefault(emptyList())

/** Android 10+: straight into Movies/ActionCam through MediaStore, so the gallery sees it. */
private class MediaStoreSink(private val uri: Uri, private val out: OutputStream) : DownloadSink {
    override fun write(bytes: ByteArray, length: Int) = out.write(bytes, 0, length)
    override fun finish(): String {
        out.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ctx.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        }
        return uri.toString()
    }
    override fun abort() {
        runCatching { out.close() }
        runCatching { ctx.contentResolver.delete(uri, null, null) }
    }
}

/** Older Android: the app's own Movies folder (no storage permission needed). */
private class FileSink(private val file: File) : DownloadSink {
    private val out = file.outputStream()
    override fun write(bytes: ByteArray, length: Int) = out.write(bytes, 0, length)
    override fun finish(): String { out.close(); return file.absolutePath }
    override fun abort() { runCatching { out.close() }; file.delete() }
}

actual fun createDownload(fileName: String): DownloadSink {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ActionCam")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Can't create $fileName")
        return MediaStoreSink(uri, ctx.contentResolver.openOutputStream(uri) ?: error("Can't write $fileName"))
    }
    val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "ActionCam").apply { mkdirs() }
    return FileSink(File(dir, fileName))
}

actual fun downloadExists(ref: String): Boolean =
    if (ref.startsWith("content://")) runCatching {
        ctx.contentResolver.query(Uri.parse(ref), arrayOf(MediaStore.Video.Media._ID), null, null, null)?.use { it.count > 0 } ?: false
    }.getOrDefault(false)
    else File(ref).exists()

actual fun openVideo(ref: String) {
    val uri = if (ref.startsWith("content://")) Uri.parse(ref)
    else FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(ref))
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(Intent.createChooser(view, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) =
    androidx.activity.compose.BackHandler(enabled, onBack)
