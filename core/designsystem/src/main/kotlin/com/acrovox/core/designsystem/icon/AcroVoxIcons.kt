package com.acrovox.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icônes de l'app. Les écrans passent par cet objet et non par `Icons.*`,
 * pour pouvoir changer de jeu d'icônes (Material Symbols) en un seul endroit.
 */
object AcroVoxIcons {
    val Play: ImageVector = Icons.Rounded.PlayArrow
    val Pause: ImageVector = Icons.Rounded.Pause
    val Replay10: ImageVector = Icons.Rounded.Replay10
    val Forward30: ImageVector = Icons.Rounded.Forward30
    val SkipPrevious: ImageVector = Icons.Rounded.SkipPrevious
    val SkipNext: ImageVector = Icons.Rounded.SkipNext
    val Speed: ImageVector = Icons.Rounded.Speed
    val SleepTimer: ImageVector = Icons.Rounded.Bedtime

    val AddToQueue: ImageVector = Icons.AutoMirrored.Rounded.PlaylistAdd
    val Queue: ImageVector = Icons.AutoMirrored.Rounded.QueueMusic
    val RemoveFromQueue: ImageVector = Icons.Rounded.RemoveCircleOutline
    val DragHandle: ImageVector = Icons.Rounded.DragIndicator
    val Ignore: ImageVector = Icons.Rounded.Close
    val Close: ImageVector = Icons.Rounded.Close
    val Share: ImageVector = Icons.Rounded.Share
    val Favorite: ImageVector = Icons.Rounded.Favorite
    val FavoriteBorder: ImageVector = Icons.Rounded.FavoriteBorder
    val Download: ImageVector = Icons.Rounded.Download
    val Downloaded: ImageVector = Icons.Rounded.DownloadDone
    val WaitingForWifi: ImageVector = Icons.Rounded.Wifi
    val Pending: ImageVector = Icons.Rounded.Schedule
    val Stop: ImageVector = Icons.Rounded.Stop
    val Error: ImageVector = Icons.Rounded.ErrorOutline
    val Delete: ImageVector = Icons.Rounded.DeleteOutline
    val Check: ImageVector = Icons.Rounded.CheckCircle
    val More: ImageVector = Icons.Rounded.MoreVert
    val ExpandMore: ImageVector = Icons.Rounded.KeyboardArrowDown
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Link: ImageVector = Icons.Rounded.Link

    val Home: ImageVector = Icons.Rounded.Home
    val Inbox: ImageVector = Icons.Rounded.Inbox
    val Discover: ImageVector = Icons.Rounded.Explore
    val Library: ImageVector = Icons.Rounded.LibraryMusic
    val Search: ImageVector = Icons.Rounded.Search
    val Notifications: ImageVector = Icons.Rounded.Notifications
    val Settings: ImageVector = Icons.Rounded.Settings
    val Sync: ImageVector = Icons.Rounded.Sync
    val Podcast: ImageVector = Icons.Rounded.Podcasts
}
