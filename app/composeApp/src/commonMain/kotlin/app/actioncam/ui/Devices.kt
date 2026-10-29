package app.actioncam.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.actioncam.model.Device
import app.actioncam.ui.common.CamIcons
import app.actioncam.ui.common.CircleButton
import app.actioncam.ui.common.Logo
import app.actioncam.ui.common.StatusDot
import app.actioncam.ui.common.YellowButton
import app.actioncam.ui.theme.Cam
import kotlinx.coroutines.launch

/** Camera list: pull down to rescan, + to add by IP, tap to open, hold for the video library. */
@Composable
fun DevicesScreen(
    devices: List<Device>,
    scanning: Boolean,
    onRefresh: () -> Unit,
    onOpen: (Device) -> Unit,
    onLibrary: (Device) -> Unit,
    onAdd: suspend (String) -> String?,
    showAdd: Boolean = false,
) {
    var adding by remember { mutableStateOf(showAdd) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(size = 24.dp, modifier = Modifier.weight(1f))
            CircleButton(CamIcons.Plus, { adding = true }, description = "Add camera by IP")
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Cameras", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Cam.fg)
            val online = devices.count { it.online }
            Text(
                if (scanning) "Scanning the network…" else "$online online · pull down to rescan",
                color = Cam.muted, fontSize = 13.sp,
            )
        }
        val pull = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = scanning, onRefresh = onRefresh, state = pull, modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pull, isRefreshing = scanning, modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Cam.yellow, color = Cam.onYellow,
                )
            },
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(devices, key = { it.key }) { d -> DeviceCard(d, { onOpen(d) }, { onLibrary(d) }) }
                if (devices.isEmpty() && !scanning) item { EmptyDevices { adding = true } }
                if (devices.isNotEmpty()) item {
                    Text(
                        "Tap a camera to control it · hold it to see its videos",
                        color = Cam.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
    if (adding) AddDeviceDialog(onAdd, onClose = { adding = false })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(d: Device, onOpen: () -> Unit, onLibrary: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cam.card)
            .combinedClickable(onClick = onOpen, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLibrary() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(50.dp).background(if (d.online) Cam.yellow else Cam.cardHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(CamIcons.Camera, null, Modifier.size(26.dp), tint = if (d.online) Cam.onYellow else Cam.muted)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, color = Cam.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(d.online)
                Spacer(Modifier.width(6.dp))
                Text(
                    (if (d.online) "Online" else "Offline") + " · " + d.host + if (d.port != Device.DEFAULT_PORT) ":${d.port}" else "",
                    color = Cam.muted, fontSize = 13.sp, maxLines = 1,
                )
            }
        }
        if (d.recording) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Cam.red).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).background(androidx.compose.ui.graphics.Color.White, CircleShape))
                Spacer(Modifier.width(5.dp))
                Text("REC", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyDevices(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Cam.card).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(CamIcons.Camera, null, Modifier.size(40.dp), tint = Cam.muted)
        Spacer(Modifier.height(12.dp))
        Text("No cameras found", color = Cam.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Make sure the camera is on the same Wi-Fi, then pull down to scan again.",
            color = Cam.muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(onClick = onAdd) { Text("Add by IP", color = Cam.yellow, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AddDeviceDialog(onAdd: suspend (String) -> String?, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun submit() {
        if (busy || text.isBlank()) return
        busy = true; error = null
        scope.launch {
            error = onAdd(text)
            busy = false
            if (error == null) onClose()
        }
    }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Cam.card).padding(22.dp)) {
            Text("Add camera", color = Cam.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Its IP address on this Wi-Fi, for example 192.168.1.131", color = Cam.muted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it; error = null },
                singleLine = true, placeholder = { Text("192.168.1.131") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cam.yellow, cursorColor = Cam.yellow),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            YellowButton("Add", ::submit, busy = busy, enabled = text.isNotBlank())
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel", color = Cam.muted) }
        }
    }
}
