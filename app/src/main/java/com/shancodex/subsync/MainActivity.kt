package com.shancodex.subsync

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.Context
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer as VLCMediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private val Cyan = Color(0xFF56E7F2)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFFAAB6CC)
private val Context.playbackStore by preferencesDataStore(name = "subsync_playback")

class MainActivity : ComponentActivity() {
    private val showSplash = mutableStateOf(true)
    private val selectedVideo = mutableStateOf<Uri?>(null)
    private lateinit var videoPicker: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private val showBrowser = mutableStateOf(false)
    private lateinit var videoPermission: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) { }
                selectedVideo.value = it
            }
        }
        videoPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showBrowser.value = true
        }
        setContent {
            if (showSplash.value) {
                SubSyncSplash(onFinished = { showSplash.value = false })
            } else if (selectedVideo.value != null) {
                SubSyncHome(videoUri = selectedVideo.value, onSelectVideo = {}, onClosePlayer = { selectedVideo.value = null })
            } else if (showBrowser.value) {
                VideoBrowserScreen(onChooseVideo = { selectedVideo.value = it; showBrowser.value = false }, onBack = { showBrowser.value = false })
            } else {
                SubSyncHome(videoUri = null, onSelectVideo = {
                    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) showBrowser.value = true
                    else videoPermission.launch(permission)
                }, onClosePlayer = { selectedVideo.value = null })
            }
        }
    }
}

@Composable
private fun SubSyncSplash(onFinished: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splash-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "splash-glow"
    )
    LaunchedEffect(Unit) {
        delay(1900)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF11182B), Color(0xFF080B13), Color(0xFF17102B)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Cyan.copy(alpha = pulse), Violet.copy(alpha = 0.75f))))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("S", color = Color(0xFF08111D), fontSize = 58.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(24.dp))
            Text("SHAN SUBSYNC AI", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text("EVERY LINE. EVERY MOMENT. IN SYNC.", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(28.dp))
            SyncWaveform(pulse = pulse)
            Spacer(Modifier.height(32.dp))
            Text("POWERED BY SHAN CODEX", color = Color(0xFF77839B), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun SubSyncHome(videoUri: Uri?, onSelectVideo: () -> Unit, onClosePlayer: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan,
        secondary = Violet,
        background = Color(0xFF080B13),
        surface = Color(0xFF151B2B)
    )) {
        if (videoUri != null) {
            SubSyncPlayerScreen(videoUri = videoUri, onBack = onClosePlayer)
        } else {
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
                            onClick = onSelectVideo,
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
}

@Composable
private fun SubSyncPlayerScreen(videoUri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progressKey = remember(videoUri) { longPreferencesKey("position_" + videoUri.toString().hashCode()) }
    val title = remember(videoUri) { queryDisplayName(context, videoUri) }
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }
    var restored by remember(videoUri) { mutableStateOf(false) }

    LaunchedEffect(player, progressKey) {
        val savedPosition = context.playbackStore.data.first()[progressKey] ?: 0L
        if (savedPosition > 5_000L) player.seekTo(savedPosition)
        restored = true
    }
    LaunchedEffect(player, restored) {
        if (restored) {
            while (true) {
                delay(3_000)
                savePlaybackPosition(context, progressKey, player)
            }
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) scope.launch { savePlaybackPosition(context, progressKey, player) }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            scope.launch { savePlaybackPosition(context, progressKey, player) }
            player.release()
        }
    }

    var isFullscreen by remember(videoUri) { mutableStateOf(false) }
    var playbackError by remember(videoUri) { mutableStateOf<String?>(null) }
    var useVlcFallback by remember(videoUri) { mutableStateOf(false) }
    var selectedAspect by remember(videoUri) { mutableStateOf("Auto") }
    val activity = context as? Activity
    val oldOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        } else {
            activity?.requestedOrientation = oldOrientation
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        onDispose {
            activity?.requestedOrientation = oldOrientation
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    DisposableEffect(player) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = error.errorCodeName + ": " + (error.message ?: "Unsupported video or audio codec")
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED || error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED) {
                    useVlcFallback = true
                    player.pause()
                }
            }
        }
        player.addListener(errorListener)
        onDispose { player.removeListener(errorListener) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF11182B), Color(0xFF080B13))))
    ) {
        if (isFullscreen) {
            if (useVlcFallback) {
                VlcFallbackPlayer(videoUri, player.currentPosition, progressKey, Modifier.fillMaxSize(), selectedAspect, true, { isFullscreen = false })
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext -> PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    } },
                    update = { it.player = player; it.resizeMode = aspectResizeMode(selectedAspect) }
                )
                TextButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)
                ) { Text("EXIT FULLSCREEN  ×", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("‹  BACK", color = Cyan, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                    TextButton(onClick = { isFullscreen = true }) { Text("⛶", color = Cyan, fontSize = 22.sp) }
                }
                Spacer(Modifier.height(12.dp))
                if (useVlcFallback) {
                    VlcFallbackPlayer(videoUri, player.currentPosition, progressKey, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)), selectedAspect)
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)),
                        factory = { viewContext -> PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        } },
                        update = { it.player = player }
                    )
                }
                if (!useVlcFallback) playbackError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    GlassPanel(modifier = Modifier.fillMaxWidth(), borderColor = Color(0xFFFF6B6B)) {
                        Text("VIDEO PLAYBACK ERROR", color = Color(0xFFFF8A8A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(error, color = Color.White, fontSize = 11.sp)
                        Text("This file may use a codec not supported by the phone's current decoder.", color = Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Auto", "16:9", "9:16", "4:3", "Fit", "Fill").forEach { ratio ->
                        FilterChip(selected = selectedAspect == ratio, onClick = { selectedAspect = ratio }, label = { Text(ratio, fontSize = 9.sp) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassPanel(modifier = Modifier.fillMaxWidth(), borderColor = Cyan.copy(alpha = 0.25f)) {
                    Text("RESUME PLAYBACK", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (restored) "Playback position is saved automatically on this device."
                        else "Checking your last watched position…",
                        color = Muted, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("POWERED BY SHAN CODEX", modifier = Modifier.fillMaxWidth(), color = Color(0xFF77839B), fontSize = 9.sp, textAlign = TextAlign.Center, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun VlcFallbackPlayer(
    videoUri: Uri,
    startPosition: Long,
    progressKey: androidx.datastore.preferences.core.Preferences.Key<Long>,
    modifier: Modifier = Modifier,
    aspectMode: String = "Auto",
    fullscreen: Boolean = false,
    onExitFullscreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val libVlc = remember { LibVLC(context, arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--avcodec-hw=none")) }
    val vlcPlayer = remember { VLCMediaPlayer(libVlc) }
    val videoLayout = remember { VLCVideoLayout(context) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(startPosition) }
    var duration by remember { mutableStateOf(0L) }
    var ready by remember { mutableStateOf(false) }
    var vlcError by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember(fullscreen) { mutableStateOf(true) }

    LaunchedEffect(fullscreen, controlsVisible) {
        if (fullscreen && controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    DisposableEffect(vlcPlayer, videoLayout, videoUri) {
        var openedDescriptor: android.os.ParcelFileDescriptor? = null
        try {
            // SAF content:// URIs are not always readable by LibVLC as direct URI paths.
            // Pass the already-authorized file descriptor instead and keep it open during playback.
            openedDescriptor = context.contentResolver.openFileDescriptor(videoUri, "r")
                ?: throw IllegalStateException("Android could not open the selected video file.")
            vlcPlayer.attachViews(videoLayout, null, false, false)
            val media = Media(libVlc, openedDescriptor.fileDescriptor)
            media.setHWDecoderEnabled(false, false)
            vlcPlayer.media = media
            media.release()
            vlcPlayer.play()
            if (startPosition > 0L) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!vlcPlayer.isReleased) vlcPlayer.time = startPosition
                }, 1500)
            }
            ready = true
        } catch (error: Exception) {
            vlcError = "VLC could not open this video: " + (error.message ?: error.javaClass.simpleName)
        }
        onDispose {
            ready = false
            try { vlcPlayer.stop() } catch (_: Exception) { }
            try { vlcPlayer.detachViews() } catch (_: Exception) { }
            try { vlcPlayer.release() } catch (_: Exception) { }
            try { openedDescriptor?.close() } catch (_: Exception) { }
            libVlc.release()
        }
    }

    LaunchedEffect(vlcPlayer, aspectMode) {
        when (aspectMode) {
            "16:9", "9:16", "4:3" -> { vlcPlayer.setAspectRatio(aspectMode); vlcPlayer.setScale(0f) }
            "Fill" -> { vlcPlayer.setAspectRatio(null); vlcPlayer.setScale(1.15f) }
            else -> { vlcPlayer.setAspectRatio(null); vlcPlayer.setScale(0f) }
        }
    }

    LaunchedEffect(vlcPlayer, ready) {
        while (ready) {
            currentPosition = vlcPlayer.time.coerceAtLeast(0L)
            duration = vlcPlayer.length.coerceAtLeast(0L)
            isPlaying = vlcPlayer.isPlaying
            context.playbackStore.edit { prefs ->
                val saved = if (duration > 0L && duration - currentPosition < 10_000L) 0L else currentPosition
                prefs[progressKey] = saved
            }
            delay(1_000)
        }
    }

    Box(
        modifier = modifier.background(Color.Black).pointerInput(fullscreen) {
            detectTapGestures(onTap = { if (fullscreen) controlsVisible = true })
        }
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { videoLayout }, update = { })
        if (!fullscreen || controlsVisible) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(if (fullscreen) Color(0xCC080B13) else Color(0xEE101010))
                    .padding(horizontal = 14.dp, vertical = if (fullscreen) 10.dp else 8.dp)
            ) {
                if (fullscreen) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onExitFullscreen) {
                            Text("EXIT FULLSCREEN  ×", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                vlcError?.let { Text(it, color = Color(0xFFFF8A8A), fontSize = 11.sp) }
                Slider(
                    value = if (duration > 0L) currentPosition.coerceIn(0L, duration).toFloat() else 0f,
                    onValueChange = { currentPosition = it.toLong() },
                    onValueChangeFinished = { if (duration > 0L) vlcPlayer.time = currentPosition },
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Cyan)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = { vlcPlayer.time = (vlcPlayer.time - 5_000L).coerceAtLeast(0L); controlsVisible = true }) {
                        Text("−5s", color = Color.White)
                    }
                    TextButton(onClick = { if (vlcPlayer.isPlaying) vlcPlayer.pause() else vlcPlayer.play(); controlsVisible = true }) {
                        Text(if (isPlaying) "Ⅱ  PAUSE" else "▶  PLAY", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { vlcPlayer.time = (vlcPlayer.time + 5_000L).coerceAtMost(duration); controlsVisible = true }) {
                        Text("+5s", color = Color.White)
                    }
                }
                Text(
                    "VLC COMPATIBILITY MODE  •  " + formatTime(currentPosition) + " / " + formatTime(duration),
                    color = Color(0xFFB8C2D8), fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VideoBrowserScreen(onChooseVideo: (Uri) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<VideoEntry>>(emptyList()) }
    var folder by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { videos = queryVideos(context) }
    val visible = videos.filter { folder == null || it.folder == folder }
    Column(Modifier.fillMaxSize().background(Color(0xFF080B13)).statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (folder != null) folder = null else onBack() }) { Text("‹ BACK", color = Cyan) }
            Text(if (folder == null) "VIDEO LIBRARY" else folder!!, color = Color.White, fontWeight = FontWeight.Bold)
        }
        if (folder == null) {
            val folders = videos.map { it.folder }.distinct().sorted()
            LazyColumn {
                item { Text("FOLDERS", color = Cyan, fontSize = 10.sp) }
                items(folders) { name ->
                    TextButton(onClick = { folder = name }, modifier = Modifier.fillMaxWidth()) { Text("▣  $name  ·  ${videos.count { it.folder == name }} videos", color = Color.White) }
                }
                item { Text("ALL VIDEOS", color = Cyan, fontSize = 10.sp) }
                items(videos) { video ->
                    TextButton(onClick = { onChooseVideo(video.uri) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(video.name, color = Color.White, maxLines = 2); Text(video.folder + " · " + formatTime(video.duration), color = Muted, fontSize = 10.sp) }
                    }
                }
            }
        } else {
            LazyColumn {
                items(visible) { video ->
                    TextButton(onClick = { onChooseVideo(video.uri) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(video.name, color = Color.White, maxLines = 2); Text(formatTime(video.duration), color = Muted, fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}

private data class VideoEntry(val uri: Uri, val name: String, val folder: String, val duration: Long)

private fun queryVideos(context: Context): List<VideoEntry> {
    val result = mutableListOf<VideoEntry>()
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.RELATIVE_PATH, MediaStore.Video.Media.DURATION)
    context.contentResolver.query(collection, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val pathCol = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
        val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol) ?: "Video"
            val path = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else "Videos/"
            val folderName = path.trimEnd('/').substringAfterLast('/').ifBlank { "Internal storage" }
            val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
            result.add(VideoEntry(Uri.withAppendedPath(collection, id.toString()), name, folderName, duration))
        }
    }
    return result
}

private fun aspectResizeMode(mode: String): Int = when (mode) {
    "Fill" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    "16:9", "9:16", "4:3" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
    else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private suspend fun savePlaybackPosition(context: Context, key: androidx.datastore.preferences.core.Preferences.Key<Long>, player: ExoPlayer) {
    val duration = player.duration
    val position = if (duration > 0L && duration - player.currentPosition < 10_000L) 0L
        else player.currentPosition.coerceAtLeast(0L)
    context.playbackStore.edit { preferences -> preferences[key] = position }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "Selected video"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) name = cursor.getString(index) ?: name
        }
    }
    return name
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
