package app.actioncam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.actioncam.model.Device
import app.actioncam.model.Video
import app.actioncam.ui.common.CamIcons
import app.actioncam.ui.common.TopBar
import app.actioncam.ui.theme.Cam
import app.actioncam.util.formatSize
import app.actioncam.util.videoTitle

/** Videos stored on a camera. Nothing is downloaded until a video is tapped. */
@Composable
fun LibraryScreen(
    device: Device,
    videos: List<Video>?,
    loading: Boolean,
    error: String?,
    progressOf: (Video) -> Float?,
    isDownloaded: (Video) -> Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTap: (Video) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Videos", subtitle = device.name, onBack = onBack)
        val pull = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = loading && videos != null, onRefresh = onRefresh, state = pull, modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pull, isRefreshing = loading && videos != null, modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Cam.yellow, color = Cam.onYellow,
                )
            },
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    videos == null && loading -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Cam.yellow)
                        }
                    }
                    error != null && videos == null -> item { Message("Camera not reachable", error, "Try again", onRefresh) }
                    videos.isNullOrEmpty() -> item { Message("No videos yet", "Start a recording with REC on the camera screen.", null, null) }
                    else -> {
                        var lastDate: String? = null
                        videos.forEach { v ->
                            val (date, _) = videoTitle(v.name)
                            if (date != lastDate) {
                                lastDate = date
                                item(key = "h-$date") {
                                    Text(date, color = Cam.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 10.dp, start = 4.dp))
                                }
                            }
                            item(key = v.name) { VideoRow(v, progressOf(v), isDownloaded(v)) { onTap(v) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(v: Video, progress: Float?, downloaded: Boolean, onTap: () -> Unit) {
    val (date, time) = videoTitle(v.name)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Cam.card).clickable(onClick = onTap).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Telegram-style file button: download -> progress (tap cancels) -> play
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when {
                v.recording -> Box(Modifier.size(48.dp).background(Cam.cardHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(14.dp).background(Cam.red, CircleShape))
                }
                progress != null -> {
                    Box(Modifier.size(48.dp).background(Cam.cardHigh, CircleShape))
                    if (progress < 0f) CircularProgressIndicator(Modifier.size(48.dp), color = Cam.yellow, strokeWidth = 3.dp)
                    else CircularProgressIndicator({ progress.coerceIn(0.02f, 1f) }, Modifier.size(48.dp), color = Cam.yellow, strokeWidth = 3.dp, trackColor = Cam.cardHigh)
                    Icon(CamIcons.Close, "Cancel", Modifier.size(20.dp), tint = Cam.fg)
                }
                downloaded -> Box(Modifier.size(48.dp).background(Cam.yellow, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(CamIcons.Play, "Play", Modifier.size(22.dp), tint = Cam.onYellow)
                }
                else -> Box(Modifier.size(48.dp).background(Cam.cardHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(CamIcons.Download, "Download", Modifier.size(24.dp), tint = Cam.yellow)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(time ?: date, color = Cam.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            val status = when {
                v.recording -> "Recording now"
                progress != null && progress >= 0f -> "${formatSize((v.size * progress).toLong())} of ${formatSize(v.size)}"
                progress != null -> "Downloading…"
                downloaded -> "${formatSize(v.size)} · on phone"
                else -> formatSize(v.size)
            }
            Text(status, color = if (downloaded && progress == null) Cam.yellow.copy(alpha = 0.85f) else Cam.muted, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Message(title: String, text: String, action: String?, onAction: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(Cam.card).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(CamIcons.Film, null, Modifier.size(40.dp), tint = Cam.muted)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Cam.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = Cam.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action, color = Cam.yellow, fontWeight = FontWeight.Bold) }
    }
}
