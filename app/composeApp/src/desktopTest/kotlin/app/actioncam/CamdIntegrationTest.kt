package app.actioncam

import app.actioncam.data.CamApi
import app.actioncam.data.Discovery
import app.actioncam.model.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Talks to a real camd. Skipped unless CAMD_HOST is set, e.g. with the test server on the laptop:
 *   CAMD_NAME="Laptop test cam" python3 server/camd.py &
 *   CAMD_HOST=127.0.0.1 ./gradlew :composeApp:desktopTest
 */
class CamdIntegrationTest {
    private val host = System.getenv("CAMD_HOST")

    @Test fun discoveryFindsTheCamera() = runBlocking {
        if (host == null) return@runBlocking
        val found = Discovery(CamApi()).scan()
        println("found: $found")
        assertTrue(found.isNotEmpty(), "no camera found")
    }

    @Test fun photoRecordListDownload() = runBlocking {
        if (host == null) return@runBlocking
        val api = CamApi()
        val d = Device(host)
        val jpeg = api.takePhoto(d)
        assertTrue(jpeg.size > 1000 && jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte(), "not a JPEG")
        assertTrue(api.lastPhoto(d)!!.contentEquals(jpeg))

        val name = "2026-01-02_03-04-05"
        assertTrue(api.startRecording(d, name).recording)
        delay(2_500)
        assertTrue(!api.stopRecording(d).recording)

        val video = api.videos(d).first { it.name.startsWith(name) }
        val out = ByteArrayOutputStream()
        var last = 0L
        api.download(d, video, { b, n -> out.write(b, 0, n) }) { done, _ -> last = done }
        assertTrue(out.size().toLong() == video.size && last == video.size, "downloaded ${out.size()} of ${video.size}")
        println("downloaded ${video.name}: ${out.size()} bytes")
    }
}
