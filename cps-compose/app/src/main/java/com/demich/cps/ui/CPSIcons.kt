package com.demich.cps.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.ReadMore
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.demich.cps.R
import com.demich.cps.contests.database.Contest

object CPSIcons {
    val Account get() = Icons.Rounded.Person
    val Accounts get() = Icons.Rounded.PeopleAlt
    val Community get() = Icons.Rounded.Newspaper //Before: Icons.Default.Subtitles
    val Contest get() = Icons.Filled.EmojiEvents
    val Development get() = Icons.Default.AllOut
    val Settings get() = Icons.Default.Settings
    val SettingsUI get() = Icons.Filled.SettingsApplications
    val Reload get() = Icons.Rounded.Refresh
    val Add get() = Icons.Outlined.AddBox
    val Close get() = Icons.Default.Close
    val Done get() = Icons.Default.Done
    val Help get() = Icons.AutoMirrored.Filled.HelpOutline
    val Info get() = Icons.Outlined.Info
    val More get() = Icons.Default.MoreVert
    val Search get() = Icons.Default.Search
    val Delete get() = Icons.Rounded.DeleteForever
    val OpenInBrowser get() = Icons.AutoMirrored.Filled.ExitToApp
    val DarkLight get() = Icons.Default.BrightnessMedium
    val DarkLightAuto get() = Icons.Default.BrightnessAuto
    val Colors get() = Icons.Default.ColorLens
    val StatusBar get() = Icons.Default.WebAsset
    val Origin get() = Icons.Default.Photo
    val Expand get() = Icons.Default.UnfoldMore
    val CollapseUp get() = Icons.Default.ExpandLess
    val ExpandDown get() = Icons.Default.ExpandMore
    val Reorder get() = Icons.Default.Reorder
    val ReorderDone get() = Icons.AutoMirrored.Filled.PlaylistAddCheck
    val MoveUp get() = Icons.Default.ArrowDropUp
    val MoveDown get() = Icons.Default.ArrowDropDown
    val RatingGraph get() = Icons.Default.Timeline
    val RatingUp get() = Icons.AutoMirrored.Filled.TrendingUp
    val RatingDown get() = Icons.AutoMirrored.Filled.TrendingDown
    val Insert get() = Icons.AutoMirrored.Rounded.ReadMore
    val EditList get() = Icons.Default.EditNote
    val SetupLoaders get() = Icons.Default.PermDataSetting
    val Star get() = Icons.Default.Star
    val Comments get() = Icons.Rounded.Forum
    val CommentSingle get() = Icons.Rounded.ChatBubble
    val BlogEntry get() = Icons.AutoMirrored.Filled.Wysiwyg
    val ArrowRight get() = Icons.AutoMirrored.Filled.ArrowRightAlt
    val ArrowBack get() = Icons.AutoMirrored.Filled.ArrowBack
    val Attention get() = Icons.Default.ErrorOutline
    val NewsFeeds get() = Icons.Default.RssFeed
    val Monitor get() = Icons.AutoMirrored.Filled.Dvr
    val Upsolving get() = Icons.Default.FitnessCenter
    val Swap get() = Icons.Default.SwapHoriz
}


@Composable
fun platformIconPainter(platform: Contest.Platform): Painter {
    val iconId = when (platform) {
        Contest.Platform.unknown -> return rememberVectorPainter(CPSIcons.Contest)
        Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
        Contest.Platform.atcoder -> R.drawable.ic_logo_atcoder
        Contest.Platform.topcoder -> R.drawable.ic_logo_topcoder
        Contest.Platform.codechef -> R.drawable.ic_logo_codechef
        Contest.Platform.dmoj -> R.drawable.ic_logo_dmoj
    }
    return painterResource(iconId)
}