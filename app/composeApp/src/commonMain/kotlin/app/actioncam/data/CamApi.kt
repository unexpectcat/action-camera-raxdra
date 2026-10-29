package app.actioncam.data

import app.actioncam.model.CamInfo
import app.actioncam.model.Device
import app.actioncam.model.Video
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CamError(message: String) : Exception(message)

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Talks to camd on the camera (see server/camd.py). */
class CamApi {
    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 30_000
        }
        expectSuccess = false
    }

    /** Short timeouts: used by scans, where most addresses never answer. */
    suspend fun probe(host: String, port: Int = Device.DEFAULT_PORT): CamInfo? = runCatching {
        val r = http.get("http://$host:$port/api/info") {
            timeout { connectTimeoutMillis = 700; requestTimeoutMillis = 1_500 }
        }
        if (r.status.isSuccess()) json.decodeFromString<CamInfo>(r.bodyAsText()) else null
    }.getOrNull()

    suspend fun info(d: Device): CamInfo = json.decodeFromString(ok(http.get("${d.baseUrl}/api/info")).bodyAsText())

    /** Asks the camera to take a photo now and returns the JPEG. */
    suspend fun takePhoto(d: Device): ByteArray = ok(http.post("${d.baseUrl}/api/photo")).bodyAsBytes()

    /** The last photo the camera took, or null if it has none. */
    suspend fun lastPhoto(d: Device): ByteArray? {
        val r = http.get("${d.baseUrl}/api/photo")
        return if (r.status.value == 404) null else ok(r).bodyAsBytes()
    }

    /** [name] is the phone's local time, so videos are named by when they were recorded. */
    suspend fun startRecording(d: Device, name: String): CamInfo =
        json.decodeFromString(ok(http.post("${d.baseUrl}/api/record/start") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name"}""")
        }).bodyAsText())

    suspend fun stopRecording(d: Device): CamInfo =
        json.decodeFromString(ok(http.post("${d.baseUrl}/api/record/stop")).bodyAsText())

    suspend fun videos(d: Device): List<Video> =
        json.decodeFromString(ok(http.get("${d.baseUrl}/api/videos")).bodyAsText())

    /** Streams a video into [write]; [progress] gets bytes so far and the total (or -1). */
    suspend fun download(d: Device, video: Video, write: (ByteArray, Int) -> Unit, progress: (Long, Long) -> Unit) {
        http.prepareGet("${d.baseUrl}/api/videos/${video.name.encodeURLPathPart()}") {
            timeout { requestTimeoutMillis = Long.MAX_VALUE }
        }.execute { r ->
            ok(r)
            val total = r.contentLength() ?: video.size
            val channel = r.bodyAsChannel()
            val buf = ByteArray(64 * 1024)
            var done = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = channel.readAvailable(buf, 0, buf.size)
                if (n < 0) break
                if (n == 0) continue
                write(buf, n)
                done += n
                progress(done, total)
            }
        }
    }

    private suspend fun ok(r: HttpResponse): HttpResponse {
        if (r.status.isSuccess()) return r
        val msg = runCatching {
            json.parseToJsonElement(r.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content
        }.getOrNull()
        throw CamError(msg ?: "Camera answered ${r.status.value}")
    }
}
