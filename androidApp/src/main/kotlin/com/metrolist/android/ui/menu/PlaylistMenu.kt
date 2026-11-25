package com.metrolist.android.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.cancel
import com.metrolist.shared.generated.resources.crossfade_duration
import com.metrolist.shared.generated.resources.crossfade_style
import com.metrolist.shared.generated.resources.crossfade_style_cut
import com.metrolist.shared.generated.resources.crossfade_style_drop
import com.metrolist.shared.generated.resources.crossfade_style_fade
import com.metrolist.shared.generated.resources.crossfade_style_rise
import com.metrolist.shared.generated.resources.playlist_mix_crossfade_disabled
import com.metrolist.shared.generated.resources.playlist_mix_custom_settings
import com.metrolist.shared.generated.resources.playlist_mix_global_settings
import com.metrolist.shared.generated.resources.playlist_mix_settings
import com.metrolist.shared.generated.resources.save
import com.metrolist.shared.generated.resources.seconds
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playback.CrossfadeStyle
import com.metrolist.shared.playback.PlaylistCrossfadeSettings
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.PlayerMenuAction
import com.metrolist.shared.ui.components.localizedBuiltinPlaylist
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.menu.MediaDownloadState
import com.metrolist.shared.ui.menu.MediaMenuSheetContent
import com.metrolist.shared.ui.menu.loadMediaItemSongs
import com.metrolist.shared.ui.menu.playlistMediaMenuModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun PlaylistMenu(
    playlist: PlaylistItem,
    onDismiss: () -> Unit,
    isFavorite: Boolean = false,
    onFavorite: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onHideFromLibrary: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    fallbackIcon: ImageVector? = null,
    loadSongs: suspend () -> List<SongItem> = { loadMediaItemSongs(playlist) },
    showSpeedDialAction: Boolean = true,
    hideFromLibraryLabel: String? = null,
) {
    val playlist = localizedBuiltinPlaylist(playlist)
    var showMixSettings by remember(playlist.id) { mutableStateOf(false) }
    val savedMixSettings = Settings.shared.playlistCrossfadeSettings(playlist.id)
    val mixDescription =
        savedMixSettings?.let { mix ->
            "${playlistMixStyleLabel(mix.style)} · ${pluralStringResource(
                Res.plurals.seconds,
                mix.durationSeconds.roundToInt(),
                mix.durationSeconds.roundToInt(),
            )}"
        } ?: stringResource(Res.string.playlist_mix_global_settings)
    val download = rememberMediaDownloadAction(playlist, onDismiss, onDownload, loadSongs)
    val pin = rememberSpeedDialToggle(playlist)
    val model =
        playlistMediaMenuModel(
            playlist = playlist,
            isFavorite = isFavorite,
            onFavorite = onFavorite,
            onDismiss = { if (!download.isConfirming()) onDismiss() },
            fallbackIcon = fallbackIcon,
            isPinned = pin.isPinned,
            showPinAction = showSpeedDialAction,
            downloadState = if (download.isDownloaded) MediaDownloadState.DOWNLOADED else MediaDownloadState.NOT_DOWNLOADED,
            onPlay = onPlay,
            onShuffle = onShuffle,
            onShare = onShare.takeUnless { playlist.isLocal || playlist.id.startsWith("auto:") },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onTogglePin = pin.toggle,
            onDownload = download.invoke,
            onHideFromLibrary = onHideFromLibrary,
            hideFromLibraryLabel = hideFromLibraryLabel,
            onExport = onExport,
            onRename = onRename,
            onDelete = onDelete,
            additionalActions =
                listOf(
                    PlayerMenuAction(
                        icon = MetrolistIcons.Outlined.Tune,
                        title = stringResource(Res.string.playlist_mix_settings),
                        description = mixDescription,
                        onClick = { showMixSettings = true },
                    ),
                ),
        )
    MediaMenuBottomSheet(model = model, onDismissRequest = onDismiss, canDismiss = { !download.isConfirming() }) {
        MediaMenuSheetContent(model)
    }
    download.confirmationDialog(onDismiss)
    if (showMixSettings) {
        PlaylistMixSettingsDialog(playlist, onDismiss = { showMixSettings = false })
    }
}

@Composable
private fun playlistMixStyleLabel(style: CrossfadeStyle): String =
    stringResource(
        when (style) {
            CrossfadeStyle.FADE -> Res.string.crossfade_style_fade
            CrossfadeStyle.RISE -> Res.string.crossfade_style_rise
            CrossfadeStyle.DROP -> Res.string.crossfade_style_drop
            CrossfadeStyle.CUT -> Res.string.crossfade_style_cut
        },
    )

@Composable
private fun PlaylistMixSettingsDialog(
    playlist: PlaylistItem,
    onDismiss: () -> Unit,
) {
    val settings = Settings.shared
    val saved = remember(playlist.id) { settings.playlistCrossfadeSettings(playlist.id) }
    var useCustomSettings by remember(playlist.id) { mutableStateOf(saved != null) }
    var selectedStyle by remember(playlist.id) {
        mutableStateOf(
            saved?.style
                ?: CrossfadeStyle.entries.firstOrNull { it.name == settings.crossfadeStyle }
                ?: CrossfadeStyle.FADE,
        )
    }
    var durationSeconds by remember(playlist.id) {
        mutableFloatStateOf(saved?.durationSeconds ?: settings.crossfadeDuration)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.playlist_mix_settings)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.playlist_mix_custom_settings),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = useCustomSettings,
                        onCheckedChange = { useCustomSettings = it },
                    )
                }
                if (useCustomSettings) {
                    Text(stringResource(Res.string.crossfade_style))
                    CrossfadeStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedStyle = style },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                            )
                            Text(playlistMixStyleLabel(style))
                        }
                    }
                    Text(stringResource(Res.string.crossfade_duration))
                    Text(
                        pluralStringResource(
                            Res.plurals.seconds,
                            durationSeconds.roundToInt(),
                            durationSeconds.roundToInt(),
                        ),
                    )
                    Slider(
                        value = durationSeconds,
                        onValueChange = { durationSeconds = it },
                        valueRange = 1f..15f,
                        steps = 13,
                    )
                } else {
                    Text(stringResource(Res.string.playlist_mix_global_settings))
                }
                if (!settings.crossfadeEnabled) {
                    Text(stringResource(Res.string.playlist_mix_crossfade_disabled))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    settings.setPlaylistCrossfadeSettings(
                        playlist.id,
                        if (useCustomSettings) {
                            PlaylistCrossfadeSettings(
                                style = selectedStyle,
                                durationSeconds = durationSeconds.roundToInt().toFloat(),
                            )
                        } else {
                            null
                        },
                    )
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
