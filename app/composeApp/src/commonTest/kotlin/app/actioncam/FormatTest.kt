package app.actioncam

import app.actioncam.util.formatDuration
import app.actioncam.util.formatSize
import app.actioncam.util.nowLocal
import app.actioncam.util.videoTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormatTest {
    @Test fun videoNames() {
        assertEquals("29 Oct 2026" to "12:30:05", videoTitle("2026-10-29_12-30-05.mp4"))
        assertEquals("29 Oct 2026" to "12:30:05 (2)", videoTitle("2026-10-29_12-30-05_2.mp4"))
        assertEquals("holiday" to null, videoTitle("holiday.mp4"))
    }

    @Test fun nowLocalIsAValidVideoName() {
        assertTrue(Regex("""\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}""").matches(nowLocal()), nowLocal())
    }

    @Test fun sizesAndDurations() {
        assertEquals("889 KB", formatSize(911_000))
        assertEquals("178.7 MB", formatSize(187_400_000))
        assertEquals("1.2 GB", formatSize(1_250_000_000))
        assertEquals("02:15", formatDuration(135))
        assertEquals("1:02:05", formatDuration(3725))
    }
}
