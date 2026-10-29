package app.actioncam.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.actioncam.ui.theme.Cam
import kotlinx.coroutines.delay

/** The app's wordmark. Replace this composable when the real logo arrives. */
@Composable
fun Logo(size: Dp = 34.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // lens mark
        Box(
            Modifier.size(size * 0.9f).border(size * 0.14f, Cam.yellow, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(size * 0.28f).background(Cam.yellow, CircleShape)) }
        Spacer(Modifier.width(size * 0.3f))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Cam.fg)) { append("action") }
                withStyle(SpanStyle(color = Cam.yellow)) { append("cam") }
            },
            fontSize = (size.value * 0.82f).sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp,
        )
    }
}

/** Full-width yellow pill, the main action of a screen. */
@Composable
fun YellowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, busy: Boolean = false, enabled: Boolean = true) {
    val active = enabled && !busy
    Box(
        modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
            .background(if (enabled) Cam.yellow else Cam.cardHigh)
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Cam.onYellow, strokeWidth = 2.5.dp)
        else Text(text.uppercase(), color = if (enabled) Cam.onYellow else Cam.muted, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
    }
}

/** Round icon button: yellow for back/add, dark otherwise. */
@Composable
fun CircleButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, yellow: Boolean = true, size: Dp = 40.dp, description: String? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(if (yellow) Cam.yellow else Cam.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(size * 0.55f), tint = if (yellow) Cam.onYellow else Cam.fg) }
}

/** Back button + title (+ optional subtitle) + whatever sits on the right. */
@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, right: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CircleButton(CamIcons.Back, onBack, description = "Back")
            Spacer(Modifier.width(14.dp))
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Cam.fg, maxLines = 1)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Cam.muted, maxLines = 1)
        }
        right()
    }
}

@Composable
fun StatusDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(if (online) Cam.green else Cam.muted.copy(alpha = 0.5f), CircleShape))
}

/** Red "REC" pill while recording, dark "REC" with a red dot when idle. */
@Composable
fun RecordPill(recording: Boolean, label: String, busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (recording) Cam.red else Cam.card)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            busy -> CircularProgressIndicator(Modifier.size(14.dp), color = Cam.fg, strokeWidth = 2.dp)
            recording -> Icon(CamIcons.Stop, "Stop", Modifier.size(14.dp), tint = Color.White)
            else -> Box(Modifier.size(12.dp).background(Cam.red, CircleShape))
        }
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

/** Message at the bottom that hides itself after a few seconds. */
@Composable
fun BoxScope.Toast(text: String?, onDone: () -> Unit) {
    LaunchedEffect(text) {
        if (text != null) { delay(2_800); onDone() }
    }
    AnimatedVisibility(
        visible = text != null,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp, start = 24.dp, end = 24.dp),
        enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Cam.cardHigh).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(text.orEmpty(), color = Cam.fg, fontSize = 14.sp)
        }
    }
}
