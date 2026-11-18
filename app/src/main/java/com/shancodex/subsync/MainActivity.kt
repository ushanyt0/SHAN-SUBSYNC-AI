package com.shancodex.subsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SubSyncHome() }
    }
}

@Composable
private fun SubSyncHome() {
    val background = Brush.verticalGradient(
        listOf(Color(0xFF10172A), Color(0xFF080B13))
    )
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier.fillMaxSize().background(background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))
            Text("SHAN", color = Color(0xFF9B8CFF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("SUBSYNC AI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Intelligent Subtitle Synchronization", color = Color(0xFFADB6CC), fontSize = 12.sp)
            Spacer(Modifier.height(40.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF19233A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Start a project", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Choose a video and a Sinhala SRT subtitle. The sync engine will analyze dialogue timing and prepare a corrected subtitle.",
                        color = Color(0xFFB8C2D8)
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("Select video")
                    }
                    OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("Select Sinhala subtitle (.srt)")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("HYBRID AI ENGINE", color = Color(0xFF9B8CFF), fontWeight = FontWeight.Bold)
            Text("Cloud processing + optional on-device analysis", color = Color(0xFFADB6CC), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("Powered by Shan Codex • v0.1.0", color = Color(0xFF66718A), fontSize = 11.sp)
        }
    }
}
