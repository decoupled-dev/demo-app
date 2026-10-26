package com.test.design.presentation.ivi.glanceables

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.test.design.presentation.assistant.VirtualAssistantActivity
import com.test.design.presentation.ivi.map.MapIntents

/** Scalable UI `apps_glance` TaskPanel — launcher grid glanceable. */
class AppsGlanceActivity : GlanceableActivity() {

    @Composable
    override fun GlanceContent() {
        GlanceRoot {
            AppsGlance(
                onOpenDashboard = {
                    startActivity(
                        MapIntents.openMain(this, openDashboard = true).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
                onOpenAssistant = { VirtualAssistantActivity.launch(this) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private enum class LauncherTarget {
    Dashboard,
    Assistant,
}

private data class LauncherApp(
    val label: String,
    val icon: ImageVector?,
    val monogram: String? = null,
    val color: Color,
    val target: LauncherTarget = LauncherTarget.Dashboard,
)

@Composable
fun AppsGlance(
    onOpenDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAssistant: () -> Unit = {},
) {
    val apps = listOf(
        LauncherApp("Search", null, "G", Color(0xFF4285F4)),
        LauncherApp("Video", Icons.Default.SmartDisplay, null, Color(0xFFFF0000)),
        LauncherApp("Store", Icons.Default.Store, null, Color(0xFF34A853)),
        LauncherApp("Camera", Icons.Default.CameraAlt, null, Color(0xFFE1306C)),
        LauncherApp("Chat", Icons.Default.Chat, null, Color(0xFF25D366)),
        LauncherApp("Music", Icons.Default.MusicNote, null, Color(0xFF1DB954)),
        LauncherApp("Watch", null, "N", Color(0xFFE50914)),
        LauncherApp(
            label = "Assistant",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF8AB4F8),
            target = LauncherTarget.Assistant,
        ),
    )
    val columns = 4
    val gap = 14.dp
    val pad = 18.dp

    val colors = glancePalette()
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = colors.card,
        border = colors.cardBorder(),
        shadowElevation = if (colors.light) 2.dp else 10.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
        ) {
            val tile = ((maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                apps.chunked(columns).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowApps.forEach { app ->
                            Box(
                                modifier = Modifier
                                    .size(tile)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(app.color.copy(alpha = 0.16f))
                                    .clickable(
                                        onClick = when (app.target) {
                                            LauncherTarget.Assistant -> onOpenAssistant
                                            LauncherTarget.Dashboard -> onOpenDashboard
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.72f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(app.color),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (app.monogram != null) {
                                        Text(
                                            text = app.monogram,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    } else if (app.icon != null) {
                                        Icon(
                                            imageVector = app.icon,
                                            contentDescription = app.label,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
