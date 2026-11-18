package com.shancodex.subsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cyan = Color(0xFF56E7F2)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFFAAB6CC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SubSyncHome() }
    }
}

@Composable
private fun SubSyncHome() {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan,
        secondary = Violet,
        background = Color(0xFF080B13),
        surface = Color(0xFF151B2B)
    )) {
        val transition = rememberInfiniteTransition(label = "subsync-glow")
        val pulse by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "glow-pulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(
                    colors = listOf(Color(0xFF11182B), Color(0xFF080B13), Color(0xFF101020))
                ))
        ) {
            Box(
                Modifier
                    .size(230.dp)
                    .offset(x = 210.dp, y = 80.dp)
                    .blur(75.dp)
                    .background(Violet.copy(alpha = pulse * 0.24f), CircleShape)
            )
            Box(
                Modifier
                    .size(190.dp)
                    .offset(x = (-90).dp, y = 420.dp)
                    .blur(75.dp)
                    .background(Cyan.copy(alpha = pulse * 0.17f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Cyan.copy(alpha = 0.28f), Violet.copy(alpha = 0.28f))))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("SHAN CODEX", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("AI WORKSPACE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF142B31))
                            .border(1.dp, Cyan.copy(alpha = 0.35f), CircleShape)
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text("BETA", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Spacer(Modifier.height(38.dp))
                Text("SUBTITLE INTELLIGENCE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(9.dp))
                Text(
                    "SHAN SUBSYNC AI",
                    color = Color.White,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Every line. Every moment. In sync.",
                    color = Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Cyan.copy(alpha = 0.25f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AUDIO", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.weight(1f))
                            Text("SUBTITLE", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        SyncWaveform(pulse = pulse)
                        Spacer(Modifier.height(15.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(Cyan, CircleShape))
                            Spacer(Modifier.width(7.dp))
                            Text("AI SYNCHRONIZATION", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(6.dp).background(Violet, CircleShape))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color.White.copy(alpha = 0.12f)
                ) {
                    Column {
                        Text("START A NEW PROJECT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Choose your video and Sinhala SRT file. We'll prepare them for accurate playback.",
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().height(51.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB9F7FA), contentColor = Color(0xFF08151B))
                        ) {
                            Text("＋   Select video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().height(51.dp),
                            shape = RoundedCornerShape(15.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Violet.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("＋   Select Sinhala subtitle (.srt)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureChip("CLOUD AI", Modifier.weight(1f), Cyan)
                    FeatureChip("OFFLINE READY", Modifier.weight(1f), Violet)
                }
                Spacer(Modifier.weight(1f))
                Text("POWERED BY SHAN CODEX", color = Color(0xFF77839B), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text("A smarter way to keep subtitles in sync  •  v0.1.0", color = Color(0xFF59647A), fontSize = 9.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(23.dp))
            .background(Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.105f), Color.White.copy(alpha = 0.035f))
            ))
            .border(1.dp, borderColor, RoundedCornerShape(23.dp))
            .padding(19.dp),
        content = content
    )
}

@Composable
private fun SyncWaveform(pulse: Float) {
    val heights = listOf(13, 22, 31, 18, 38, 27, 44, 25, 35, 48, 28, 40, 20, 34, 16, 26, 12)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        val gap = size.width / (heights.size * 1.7f)
        val barWidth = gap * 0.62f
        heights.forEachIndexed { index, height ->
            val x = gap * index + gap * 0.55f
            val barHeight = height.dp.toPx() * (0.72f + pulse * 0.28f)
            val color = if (index % 2 == 0) Cyan else Violet
            drawLine(
                color = color.copy(alpha = 0.55f + pulse * 0.45f),
                start = androidx.compose.ui.geometry.Offset(x, size.height / 2 - barHeight / 2),
                end = androidx.compose.ui.geometry.Offset(x, size.height / 2 + barHeight / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun FeatureChip(label: String, modifier: Modifier, accent: Color) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(accent, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}
