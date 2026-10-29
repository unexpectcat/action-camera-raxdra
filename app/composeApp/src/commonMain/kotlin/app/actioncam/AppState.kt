package app.actioncam

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import app.actioncam.data.CamApi
import app.actioncam.data.Discovery
import app.actioncam.data.json
import app.actioncam.data.refresh
import app.actioncam.model.CamInfo
import app.actioncam.model.Device
import app.actioncam.model.SavedState
import app.actioncam.model.Video
import app.actioncam.platform.createDownload
import app.actioncam.platform.decodeImage
import app.actioncam.platform.downloadExists
import app.actioncam.platform.openVideo
import app.actioncam.platform.readAppFile
import app.actioncam.platform.writeAppFile
import app.actioncam.util.nowLocal
import app.actioncam.util.videoBaseName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Splash : Screen
    data object Devices : Screen
    data class Camera(val device: Device) : Screen
    data class Library(val device: Device) : Screen
}

private const val STATE_FILE = "state.json"

/** Everything the screens show. Mutated on the main thread; network work runs in [scope]. */
class AppState(
    private val scope: CoroutineScope,
    private val api: CamApi = CamApi(),
    private val discovery: Discovery = Discovery(api),
) {
    var screen by mutableStateOf<Screen>(Screen.Splash)
    var devices by mutableStateOf(listOf<Device>())
    var scanning by mutableStateOf(false)
    /** Short message shown at the bottom (errors, confirmations). */
    var toast by mutableStateOf<String?>(null)

    // camera screen
    var info by mutableStateOf<CamInfo?>(null)
    var photo by mutableStateOf<ImageBitmap?>(null)
    var photoLoading by mutableStateOf(false)
    var takingPhoto by mutableStateOf(false)
    var recordBusy by mutableStateOf(false)

    // library screen
    var videos by mutableStateOf<List<Video>?>(null)
    var libraryLoading by mutableStateOf(false)
    var libraryError by mutableStateOf<String?>(null)
    /** Video key -> download progress 0..1 (-1 when the size is unknown). */
    val progress = mutableStateMapOf<String, Float>()
    /** Video key -> local reference of the saved file. */
    val downloaded = mutableStateMapOf<String, String>()
    private val downloadJobs = mutableMapOf<String, Job>()

    init {
        val saved = readAppFile(STATE_FILE)?.let { runCatching { json.decodeFromString<SavedState>(it) }.getOrNull() } ?: SavedState()
        devices = saved.devices
        downloaded.putAll(saved.downloads)
    }

    private fun save() {
        val state = SavedState(devices, downloaded.toMap())
        writeAppFile(STATE_FILE, json.encodeToString(SavedState.serializer(), state))
    }

    fun say(text: String) { toast = text }

    // ---- devices ----

    /** Pull-to-refresh: broadcast + subnet sweep, then re-check the saved ones the scan missed. */
    fun scan() {
        if (scanning) return
        scanning = true
        scope.launch {
            try {
                val found = withContext(Dispatchers.Default) { discovery.scan() }
                val foundKeys = found.map { it.key }.toSet()
                val foundNames = found.map { it.name }.toSet()
                // a camera that got a new IP shows up under its name: drop the old auto entry
                val rest = devices.filter { it.key !in foundKeys && (it.manual || it.name !in foundNames) }
                val checked = withContext(Dispatchers.Default) { api.refresh(rest) }
                val manual = devices.filter { it.manual }.map { it.key }.toSet()
                devices = (found.map { it.copy(manual = it.key in manual) } + checked)
                    .sortedWith(compareByDescending<Device> { it.online }.thenBy { it.name.lowercase() })
                save()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Scan failed: ${e.message}")
            } finally {
                scanning = false
            }
        }
    }

    /** The + button. Returns an error to show in the dialog, or null when the camera was added. */
    suspend fun addDevice(input: String): String? {
        val text = input.trim().removePrefix("http://").trimEnd('/')
        val host = text.substringBefore(':')
        val port = text.substringAfter(':', "").toIntOrNull() ?: Device.DEFAULT_PORT
        if (!Regex("""\d{1,3}(\.\d{1,3}){3}""").matches(host) && !host.contains('.')) return "Enter an IP like 192.168.1.131"
        val info = withContext(Dispatchers.Default) { api.probe(host, port) }
            ?: return "No camera answered at $host:$port"
        val d = Device(host, port, info.name.ifBlank { host }, manual = true, online = true, recording = info.recording)
        devices = listOf(d) + devices.filter { it.key != d.key }
        save()
        return null
    }

    fun forget(device: Device) {
        devices = devices.filter { it.key != device.key }
        save()
    }

    // ---- navigation ----

    fun open(device: Device) {
        info = null; photo = null
        screen = Screen.Camera(device)
        loadLastPhoto(device)
    }

    fun openLibrary(device: Device) {
        videos = null; libraryError = null
        screen = Screen.Library(device)
        loadVideos(device)
    }

    fun back() {
        screen = when (screen) {
            is Screen.Camera, is Screen.Library -> Screen.Devices
            else -> screen
        }
    }

    // ---- camera screen ----

    /** Keeps the recording state fresh while the camera screen is open. */
    suspend fun pollInfo(device: Device) {
        while (true) {
            // a missed poll keeps the last known state
            runCatching { withContext(Dispatchers.Default) { api.info(device) } }.onSuccess { info = it }
            delay(2_000)
        }
    }

    private fun loadLastPhoto(device: Device) {
        photoLoading = true
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { api.lastPhoto(device) }
                photo = bytes?.let(::decodeImage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say("Camera not reachable")
            } finally {
                photoLoading = false
            }
        }
    }

    fun takePhoto(device: Device) {
        if (takingPhoto) return
        takingPhoto = true
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) { api.takePhoto(device) }
                photo = decodeImage(bytes) ?: run { say("Camera sent a broken image"); photo }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say(e.message ?: "Photo failed")
            } finally {
                takingPhoto = false
            }
        }
    }

    fun toggleRecording(device: Device) {
        if (recordBusy) return
        recordBusy = true
        val start = info?.recording != true
        scope.launch {
            try {
                info = withContext(Dispatchers.Default) {
                    if (start) api.startRecording(device, nowLocal()) else api.stopRecording(device)
                }
                say(if (start) "Recording started" else "Recording saved")
                devices = devices.map { if (it.key == device.key) it.copy(recording = start) else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                say(e.message ?: "Camera not reachable")
            } finally {
                recordBusy = false
            }
        }
    }

    // ---- library ----

    fun videoKey(device: Device, video: Video) = "${device.key}/${video.name}"

    fun loadVideos(device: Device) {
        libraryLoading = true
        scope.launch {
            try {
                videos = withContext(Dispatchers.Default) { api.videos(device) }
                libraryError = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                libraryError = "Can't reach ${device.name}"
            } finally {
                libraryLoading = false
            }
        }
    }

    /** Telegram-style: tap downloads, tap again while downloading cancels, tap when saved opens it. */
    fun tapVideo(device: Device, video: Video) {
        val key = videoKey(device, video)
        downloadJobs[key]?.let { job ->
            job.cancel()
            return
        }
        downloaded[key]?.let { ref ->
            if (downloadExists(ref)) return openVideo(ref)
            downloaded.remove(key); save()
        }
        if (video.recording) return say("Still recording. Stop it first")
        progress[key] = 0f
        downloadJobs[key] = scope.launch {
            val sink = createDownload("${videoBaseName(video.name)}.mp4")
            try {
                withContext(Dispatchers.Default) {
                    api.download(device, video, sink::write) { done, total ->
                        val p = if (total > 0) done.toFloat() / total else -1f
                        scope.launch { progress[key] = p }
                    }
                }
                downloaded[key] = sink.finish()
                save()
            } catch (e: CancellationException) {
                sink.abort()
            } catch (e: Exception) {
                sink.abort()
                say("Download failed: ${e.message}")
            } finally {
                progress.remove(key)
                downloadJobs.remove(key)
            }
        }
    }

    fun isDownloaded(device: Device, video: Video) = downloaded.containsKey(videoKey(device, video))
}
