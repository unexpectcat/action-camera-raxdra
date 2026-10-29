package app.actioncam.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A camera the app knows about. Saved ones survive restarts; [online] is filled by scans. */
@Serializable
data class Device(
    val host: String,
    val port: Int = DEFAULT_PORT,
    val name: String = host,
    /** Added by hand with the + button (kept even when a scan doesn't find it). */
    val manual: Boolean = false,
    @kotlinx.serialization.Transient val online: Boolean = false,
    @kotlinx.serialization.Transient val recording: Boolean = false,
) {
    val key get() = "$host:$port"
    val baseUrl get() = "http://$host:$port"

    companion object {
        const val DEFAULT_PORT = 8080
        const val DISCOVERY_PORT = 41234
    }
}

/** GET /api/info, also the UDP discovery reply. */
@Serializable
data class CamInfo(
    val name: String = "",
    val source: String = "",
    val recording: Boolean = false,
    @SerialName("recording_name") val recordingName: String? = null,
    @SerialName("recording_seconds") val recordingSeconds: Long = 0,
    @SerialName("has_photo") val hasPhoto: Boolean = false,
    val port: Int = Device.DEFAULT_PORT,
)

@Serializable
data class Video(
    val name: String,
    val size: Long,
    val time: Long = 0,
    val recording: Boolean = false,
)

/** Saved on the phone: devices and which videos were downloaded where. */
@Serializable
data class SavedState(
    val devices: List<Device> = emptyList(),
    /** "host:port/name" -> local reference (content:// uri on Android, file path on desktop). */
    val downloads: Map<String, String> = emptyMap(),
)
