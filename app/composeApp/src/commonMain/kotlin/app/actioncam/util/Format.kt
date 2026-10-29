package app.actioncam.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private fun Int.pad() = toString().padStart(2, '0')

/** Phone's local time as a video name: 2026-10-29_12-30-05. */
fun nowLocal(): String {
    val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${t.year}-${t.month.ordinal.plus(1).pad()}-${t.day.pad()}_${t.hour.pad()}-${t.minute.pad()}-${t.second.pad()}"
}

fun videoBaseName(file: String) = file.removeSuffix(".mp4")

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val NAME = Regex("""(\d{4})-(\d{2})-(\d{2})_(\d{2})-(\d{2})-(\d{2})(?:_(\d+))?""")

/** "2026-10-29_12-30-05.mp4" -> ("29 Oct 2026", "12:30:05"); other names are shown as they are. */
fun videoTitle(file: String): Pair<String, String?> {
    val m = NAME.matchEntire(videoBaseName(file)) ?: return videoBaseName(file) to null
    val (y, mo, d, h, mi, s, n) = m.destructured
    val month = MONTHS.getOrNull(mo.toInt() - 1) ?: mo
    val time = "$h:$mi:$s" + if (n.isNotEmpty()) " ($n)" else ""
    return "${d.toInt()} $month $y" to time
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${oneDecimal(bytes / 1048576.0)} MB"
    else -> "${oneDecimal(bytes / 1073741824.0)} GB"
}

private fun oneDecimal(v: Double): String {
    val tenths = kotlin.math.round(v * 10).toLong()
    return "${tenths / 10}.${tenths % 10}"
}

/** 125 -> "02:05", 3725 -> "1:02:05". */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600 / 60).toInt()
    val s = (seconds % 60).toInt()
    return if (h > 0) "$h:${m.pad()}:${s.pad()}" else "${m.pad()}:${s.pad()}"
}
