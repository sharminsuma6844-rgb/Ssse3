package com.saad.materialyoumusic

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import java.util.Locale

data class Track(val uri: Uri, val title: String, val artist: String = "Local music")

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            MaterialMusicTheme {
                MusicScreen(
                    player = player,
                    onPick = { uris ->
                        val tracks = uris.map { uri ->
                            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBefore('?')
                                ?.replace(Regex("%[0-9A-Fa-f]{2}"), " ") ?: "Audio track"
                            Track(uri, name)
                        }
                        tracks
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

@Composable
private fun MaterialMusicTheme(content: @Composable () -> Unit) {
    var dark by remember { mutableStateOf(false) }
    val lightScheme = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF56539B),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        background = androidx.compose.ui.graphics.Color(0xFFF7F5FF),
        surface = androidx.compose.ui.graphics.Color(0xFFEEEBFA),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE4E0F5)
    )
    val darkScheme = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFFC5C1FF),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF27244B),
        background = androidx.compose.ui.graphics.Color(0xFF14131C),
        surface = androidx.compose.ui.graphics.Color(0xFF211F2B),
        secondaryContainer = androidx.compose.ui.graphics.Color(0xFF383447)
    )
    MaterialTheme(colorScheme = if (dark) darkScheme else lightScheme) {
        CompositionLocalProvider(LocalDarkSetter provides { dark = !dark }) { content() }
    }
}

private val LocalDarkSetter = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicScreen(player: ExoPlayer, onPick: (List<Uri>) -> List<Track>) {
    var tracks by remember { mutableStateOf(listOf<Track>()) }
    var current by remember { mutableIntStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val added = onPick(uris)
            tracks = tracks + added
            if (current < 0 && tracks.isNotEmpty()) {
                current = 0
                player.setMediaItems(tracks.map { MediaItem.fromUri(it.uri) })
                player.prepare()
            } else {
                player.setMediaItems(tracks.map { MediaItem.fromUri(it.uri) }, current.coerceAtLeast(0), 0L)
                player.prepare()
            }
        }
    }

    LaunchedEffect(player) {
        while (true) {
            playing = player.isPlaying
            position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.takeIf { it > 0 } ?: 0L
            delay(300)
        }
    }

    LaunchedEffect(shuffle) { player.shuffleModeEnabled = shuffle }
    LaunchedEffect(repeat) { player.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    LaunchedEffect(volume) { player.volume = volume }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Music", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                            Text("Your music, your mood.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { darkMode = !darkMode }) {
                        Icon(if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode, "Toggle theme")
                    }
                }
            )
        },
        bottomBar = {
            if (tracks.isNotEmpty()) {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().clickable { }.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniDisc()
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tracks.getOrNull(current)?.title ?: "Nothing playing", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(if (playing) "Playing now" else "Ready to play", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause")
                        }
                    }
                }
            }
        }
    ) { padding ->
        MaterialTheme(colorScheme = if (darkMode) darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFC5C1FF),
            onPrimary = androidx.compose.ui.graphics.Color(0xFF27244B),
            background = androidx.compose.ui.graphics.Color(0xFF14131C),
            surface = androidx.compose.ui.graphics.Color(0xFF211F2B),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFF383447)
        ) else lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF56539B),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            background = androidx.compose.ui.graphics.Color(0xFFF7F5FF),
            surface = androidx.compose.ui.graphics.Color(0xFFEEEBFA),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE4E0F5)
        )) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text("MATERIAL YOU · MUSIC", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Your sound.\nYour universe.", fontSize = 31.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Your local music collection, in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { pickAudio.launch(arrayOf("audio/*")) }) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add your music")
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Now playing", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${tracks.size} songs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(25.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Album, null, modifier = Modifier.size(105.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("♫", fontSize = 42.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                tracks.getOrNull(current)?.title ?: "Choose your music",
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                tracks.getOrNull(current)?.artist ?: "Your personal music player",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
                            )
                            Spacer(Modifier.height(18.dp))
                            Slider(
                                value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                                onValueChange = { if (duration > 0) player.seekTo(it.toLong()) },
                                valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                                enabled = duration > 0
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(timeText(position), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeText(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { shuffle = !shuffle }) {
                                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { if (tracks.isNotEmpty()) player.seekToPreviousMediaItem() }) {
                                    Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(30.dp))
                                }
                                FilledIconButton(
                                    onClick = { if (tracks.isNotEmpty()) { if (player.isPlaying) player.pause() else player.play() } else pickAudio.launch(arrayOf("audio/*")) },
                                    modifier = Modifier.size(68.dp),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", modifier = Modifier.size(34.dp))
                                }
                                IconButton(onClick = { if (tracks.isNotEmpty()) player.seekToNextMediaItem() }) {
                                    Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(30.dp))
                                }
                                IconButton(onClick = { repeat = !repeat }) {
                                    Icon(Icons.Default.RepeatOne, "Repeat current track", tint = if (repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your playlist", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            player.stop()
                            tracks = emptyList()
                            current = -1
                            player.clearMediaItems()
                        }) { Text("CLEAR") }
                    }
                }
                if (tracks.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Your playlist is waiting", fontWeight = FontWeight.SemiBold)
                                Text("Tap Add your music to select audio files.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    itemsIndexed(tracks) { index, track ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                current = index
                                player.seekTo(index, 0L)
                                player.play()
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(track.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (index == current && playing) Icon(Icons.Default.GraphicEq, "Currently playing", tint = MaterialTheme.colorScheme.primary)
                                else Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniDisc() {
    Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(29.dp))
    }
}

private fun timeText(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}
