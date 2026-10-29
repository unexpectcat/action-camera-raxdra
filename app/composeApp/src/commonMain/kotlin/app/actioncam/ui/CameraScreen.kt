package app.actioncam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.actioncam.model.CamInfo
import app.actioncam.model.Device
import app.actioncam.ui.common.CamIcons
import app.actioncam.ui.common.RecordPill
import app.actioncam.ui.common.StatusDot
import app.actioncam.ui.common.TopBar
import app.actioncam.ui.common.YellowButton
import app.actioncam.ui.theme.Cam
import app.actioncam.util.formatDuration
import app.actioncam.util.videoTitle
import kotlinx.coroutines.delay

/** One camera: the last photo it sent, "Send photo" at the bottom, record toggle top right. */
@Composable
fun CameraScreen(
    device: Device,
    info: CamInfo?,
    photo: ImageBitmap?,
    photoLoading: Boolean,
    takingPhoto: Boolean,
    recordBusy: Boolean,
    onBack: () -> Unit,
    onPhoto: () -> Unit,
    onRecord: () -> Unit,
    onLibrary: () -> Unit,
) {
    val recording = info?.recording == true
    // count locally between polls so the timer ticks every second
    var seconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(info?.recording, info?.recordingName, info?.recordingSeconds) {
        seconds = info?.recordingSeconds ?: 0
        while (info?.recording == true) { delay(1_000); seconds++ }
    }
    val online = info != null || device.online

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = info?.name?.ifBlank { null } ?: device.name,
            subtitle = device.host,
            onBack = onBack,
        ) {
            RecordPill(recording, if (recording) formatDuration(seconds) else "REC", recordBusy, onRecord)
        }

        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)).background(Cam.deep),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    photo != null -> Image(photo, "Last photo", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    photoLoading -> CircularProgressIndicator(color = Cam.yellow)
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(CamIcons.Image, null, Modifier.size(44.dp), tint = Cam.muted)
                        Spacer(Modifier.height(10.dp))
                        Text("No photo yet", color = Cam.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Tap Send photo to get one", color = Cam.muted, fontSize = 13.sp)
                    }
                }
                if (takingPhoto && photo != null) {
                    Box(Modifier.fillMaxSize().background(Cam.deep.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Cam.yellow)
                    }
                }
                if (recording) {
                    Row(
                        Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(10.dp))
                            .background(Cam.deep.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(Cam.red, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Recording", color = Cam.fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(online)
                Text(
                    when {
                        info == null && !online -> "Not reachable"
                        recording -> "Recording since ${info?.recordingName?.let { videoTitle(it).second } ?: "now"}"
                        info?.source == "test" -> "Connected · test picture (no camera module)"
                        else -> "Connected · ready"
                    },
                    color = Cam.muted, fontSize = 13.sp, maxLines = 1,
                )
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            YellowButton("Send photo", onPhoto, busy = takingPhoto)
            TextButton(onClick = onLibrary, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Videos on this camera", color = Cam.yellow, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
