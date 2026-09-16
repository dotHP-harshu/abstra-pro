package dev.abstra.pro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ui.theme.LocalAbstraColors

enum class MainNavigationTab(val title: String) {
    HOME("Home"),
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
    SETTINGS("Settings")
}

@Composable
fun TopSegmentedTabs(
    selectedTab: MainNavigationTab,
    onTabSelected: (MainNavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    /** Live pager position (currentPage + offsetFraction). When provided, the
     *  indicator pill slides continuously with swipes instead of only animating
     *  after the page settles. */
    pagerPageFraction: Float? = null
) {
    val colors = LocalAbstraColors.current
    val tabs = MainNavigationTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)

    // Compute bias from -1f to 1f for the animated sliding indicator pill
    val targetBias = if (tabs.size > 1) {
        (selectedIndex.toFloat() * 2f / (tabs.size - 1)) - 1f
    } else {
        0f
    }
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
        label = "tabIndicatorBias"
    )
    // While the pager is available, track it 1:1 (one continuous gesture system)
    val liveBias = pagerPageFraction?.let { fraction ->
        if (tabs.size > 1) (fraction * 2f / (tabs.size - 1)) - 1f else 0f
    }
    val indicatorBias = liveBias ?: animatedBias

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(6.dp, RoundedCornerShape(25.dp), spotColor = colors.primary.copy(alpha = 0.25f))
            .border(1.5.dp, colors.border, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        color = colors.bgSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Sliding animated pill background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f / tabs.size)
                    .align(BiasAlignment(horizontalBias = indicatorBias, verticalBias = 0f))
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = colors.primary.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.primary)
            )

            // Clickable tab titles
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) Color.White else colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
