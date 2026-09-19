package com.acrovox.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.acrovox.app.navigation.TopLevelDestination
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme

/**
 * Barre de navigation Pulse Audio : 64 dp, surface translucide, filet supérieur,
 * pilule violette derrière l'icône active et libellé corail.
 *
 * Le flou d'arrière-plan de la maquette n'est pas reproduit : Compose ne floute pas
 * ce qui passe derrière un composable.
 */
@Composable
fun AcroVoxBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    badgeCounts: Map<TopLevelDestination, Int> = emptyMap()
) {
    val colors = AcroVoxTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceCard.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(color = colors.outlineSubtle)
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = destination == selected,
                    badgeCount = badgeCounts[destination] ?: 0,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AcroVoxTheme.colors
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 28.dp)
                .background(
                    if (selected) colors.accent.copy(alpha = 0.2f) else colors.accent.copy(alpha = 0f),
                    AcroVoxShape.Pill
                ),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(badge = {
                if (badgeCount >
                    0
                ) {
                    Badge(containerColor = colors.brand) { Text(badgeCount.toString()) }
                }
            }) {
                Icon(
                    destination.icon,
                    contentDescription = null,
                    tint = if (selected) colors.textPrimary else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.brand else colors.textSecondary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
