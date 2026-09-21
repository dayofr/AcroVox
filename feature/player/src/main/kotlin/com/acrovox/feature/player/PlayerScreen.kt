package com.acrovox.feature.player

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.database.entity.ChapterEntity
import com.acrovox.core.designsystem.component.AcroVoxFilterChip
import com.acrovox.core.designsystem.component.Artwork
import com.acrovox.core.designsystem.component.PlayButtonSize
import com.acrovox.core.designsystem.component.PlayPauseButton
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.player.PlayerController
import com.acrovox.core.player.SleepTimerState
import kotlinx.coroutines.delay

private val speeds = listOf(0.8f, 1f, 1.1f, 1.2f, 1.3f, 1.5f, 1.75f, 2f, 2.5f, 3f)
private val sleepMinutes = listOf(5, 10, 15, 30, 45, 60, 90)
private const val SLEEP_HINT = "Arrête la lecture en douceur. Secouez le téléphone pour prolonger de 5 minutes."

@Composable
fun PlayerScreen(
    onClose: () -> Unit,
    onOpenEpisode: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = viewModel.controller
    val player = state.player
    val colors = AcroVoxTheme.colors
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(player.hasEpisode) { if (!player.hasEpisode) onClose() }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Spacing.screenMargin)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(AcroVoxIcons.ExpandMore, contentDescription = "Réduire", tint = colors.textPrimary)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Diffusion en cours", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text(
                    player.podcastTitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.brand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                context.share(
                    player.podcastTitle,
                    player.title,
                    state.episode?.episode?.link ?: state.episode?.episode?.mediaUrl
                )
            }) {
                Icon(AcroVoxIcons.Share, contentDescription = "Partager", tint = colors.textPrimary)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Artwork(
            player.artworkUrl,
            contentDescription = null,
            shape = AcroVoxShape.Card,
            modifier = Modifier.fillMaxWidth(0.86f).aspectRatio(1f).shadow(24.dp, AcroVoxShape.Card)
        )
        Spacer(Modifier.height(Spacing.xl))
        Text(
            player.title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        state.episode?.let { item ->
            Text(
                "Voir les notes de l'épisode",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = Spacing.xs).clickableText { onOpenEpisode(item.episode.id) }
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            val favorite = state.episode?.episode?.isFavorite == true
            PillButton(
                if (favorite) AcroVoxIcons.Favorite else AcroVoxIcons.FavoriteBorder,
                null,
                highlighted = favorite,
                onClick = viewModel::toggleFavorite
            )
            PillButton(
                if (state.queued) AcroVoxIcons.RemoveFromQueue else AcroVoxIcons.AddToQueue,
                "File",
                highlighted = state.queued,
                onClick = viewModel::toggleQueue
            )
            PillButton(AcroVoxIcons.Speed, formatSpeed(player.speed), highlighted = player.speed != 1f, onClick = {
                sheet =
                    "speed"
            })
            PillButton(
                AcroVoxIcons.SleepTimer,
                sleepLabel(player.sleepTimer),
                highlighted =
                player.sleepTimer != SleepTimerState.Off,
                onClick = { sheet = "sleep" }
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val chapters = state.chapters
        if (chapters.isNotEmpty()) {
            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = colors.surfaceCard,
                contentColor = colors.textPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lecture") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Chapitres (${chapters.size})") })
            }
            Spacer(Modifier.height(Spacing.md))
        } else {
            tab = 0
        }
        if (tab == 0) {
            SeekBar(player.positionMs, player.durationMs, chapters, onSeek = controller::seekTo)
            Row(
                Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(AcroVoxIcons.SkipPrevious, "Précédent", onClick = controller::previous)
                ControlButton(
                    AcroVoxIcons.Replay10,
                    "Reculer de ${state.skipBackSeconds} s",
                    onClick = controller::skipBack
                )
                PlayPauseButton(
                    isPlaying = player.isPlaying,
                    onClick = controller::togglePlayPause,
                    size = PlayButtonSize.Large,
                    modifier = Modifier.size(72.dp)
                )
                ControlButton(
                    AcroVoxIcons.Forward30,
                    "Avancer de ${state.skipForwardSeconds} s",
                    onClick = controller::skipForward
                )
                ControlButton(AcroVoxIcons.SkipNext, "Suivant dans la file", onClick = controller::next)
            }
        } else {
            ChaptersList(chapters, player.positionMs, player.durationMs, onSeek = controller::seekTo)
        }
    }

    when (sheet) {
        "speed" -> SpeedSheet(player.speed, state.skipSilence, controller, viewModel::setSkipSilence) { sheet = null }
        "sleep" -> SleepSheet(player.sleepTimer, controller) { sheet = null }
    }
}

private fun Modifier.clickableText(onClick: () -> Unit) = clickable(onClick = onClick)

@Composable
private fun SeekBar(positionMs: Long, durationMs: Long, chapters: List<ChapterEntity>, onSeek: (Long) -> Unit) {
    val colors = AcroVoxTheme.colors
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = durationMs.coerceAtLeast(1)
    val shown = if (dragging) (dragValue * duration).toLong() else positionMs
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = if (dragging) dragValue else (positionMs.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeek((dragValue * duration).toLong())
                dragging = false
            },
            enabled = durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = colors.brand,
                activeTrackColor = colors.brand,
                inactiveTrackColor = colors.outlineSubtle
            )
        )
        if (chapters.size > 1 && durationMs > 0) {
            ChapterTicks(chapters, durationMs)
        }
        Row {
            Text(formatClock(shown), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                "-" + formatClock((durationMs - shown).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ChapterTicks(chapters: List<ChapterEntity>, durationMs: Long) {
    val color = AcroVoxTheme.colors.textMuted
    Canvas(
        Modifier.fillMaxWidth().height(6.dp).padding(horizontal = 12.dp)
    ) {
        chapters.forEach { chapter ->
            val x = size.width * (chapter.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        }
    }
}

/** Onglet Chapitres : liste, chapitre courant surligné, saut au tap, images. */
@Composable
fun ChaptersList(
    chapters: List<ChapterEntity>,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AcroVoxTheme.colors
    val currentIndex = chapters.indexOfLast { it.startMs <= positionMs }.coerceAtLeast(0)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        chapters.forEachIndexed { index, chapter ->
            val endMs = chapters.getOrNull(index + 1)?.startMs ?: durationMs.coerceAtLeast(chapter.startMs)
            val selected = index == currentIndex
            Surface(
                onClick = { onSeek(chapter.startMs) },
                shape = AcroVoxShape.Card,
                color = if (selected) colors.surfaceFloating else colors.surfaceCard,
                border = BorderStroke(1.dp, if (selected) colors.brand else colors.outlineSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(
                        url = chapter.imageUrl,
                        contentDescription = null,
                        shape = AcroVoxShape.ArtworkSmall,
                        modifier = Modifier.size(48.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatClock(chapter.startMs) + " · " +
                                formatClock((endMs - chapter.startMs).coerceAtLeast(0)),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    if (selected) {
                        Icon(
                            AcroVoxIcons.Play,
                            contentDescription = "Chapitre en cours",
                            tint = colors.brand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = AcroVoxTheme.colors.textPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun PillButton(icon: ImageVector, label: String?, highlighted: Boolean, onClick: () -> Unit) {
    val colors = AcroVoxTheme.colors
    Surface(onClick = onClick, shape = AcroVoxShape.Pill, color = colors.surfaceFloating) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (highlighted) colors.brand else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            if (label != null) {
                Text(
                    " $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (highlighted) colors.brand else colors.textPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(
    current: Float,
    skipSilence: Boolean,
    controller: PlayerController,
    onSkipSilence: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AcroVoxTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceModal) {
        Column(
            Modifier.padding(horizontal = Spacing.screenMargin).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("Vitesse de lecture", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(speeds) { speed ->
                    AcroVoxFilterChip(formatSpeed(speed), selected = speed == current, onClick = {
                        controller.setSpeed(speed)
                    })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Raccourcir les silences",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        "Saute les blancs sans changer la voix.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = skipSilence,
                    onCheckedChange = onSkipSilence,
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.brand)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepSheet(timer: SleepTimerState, controller: PlayerController, onDismiss: () -> Unit) {
    val colors = AcroVoxTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceModal) {
        Column(
            Modifier.padding(horizontal = Spacing.screenMargin).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("Minuteur de sommeil", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(
                when (timer) {
                    SleepTimerState.Off -> SLEEP_HINT
                    SleepTimerState.EndOfEpisode -> "Arrêt à la fin de l'épisode."
                    is SleepTimerState.Until -> "Arrêt dans ${sleepLabel(timer)}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(sleepMinutes) { minutes ->
                    AcroVoxFilterChip("$minutes min", selected = false, onClick = {
                        controller.setSleepTimer(minutes)
                        onDismiss()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AcroVoxFilterChip("Fin de l'épisode", selected = timer == SleepTimerState.EndOfEpisode, onClick = {
                    controller.setSleepAtEndOfEpisode()
                    onDismiss()
                })
                if (timer != SleepTimerState.Off) {
                    AcroVoxFilterChip("Désactiver", selected = false, onClick = {
                        controller.cancelSleepTimer()
                        onDismiss()
                    })
                }
            }
        }
    }
}

/** Minutes restantes, recalculées chaque seconde. */
@Composable
private fun sleepLabel(timer: SleepTimerState): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (timer is SleepTimerState.Until) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return when (timer) {
        SleepTimerState.Off -> "Minuteur"
        SleepTimerState.EndOfEpisode -> "Fin d'épisode"
        is SleepTimerState.Until -> {
            val seconds = ((timer.endsAt - now) / 1000).coerceAtLeast(0)
            if (seconds >= 60) "${(seconds + 59) / 60} min" else "$seconds s"
        }
    }
}

internal fun formatSpeed(speed: Float): String =
    "${"%.2f".format(java.util.Locale.US, speed).trimEnd('0').trimEnd('.')}x"

/** « 4:05 », « 1:02:03 ». */
internal fun formatClock(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun Context.share(podcast: String, title: String, url: String?) {
    val text = listOfNotNull("$podcast : $title", url).joinToString("\n")
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            "Partager l'épisode"
        )
    )
}
