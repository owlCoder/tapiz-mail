package rs.tapizlabs.mail.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import rs.tapizlabs.mail.ui.theme.AppColors

private data class BottomNavEntry(
    val tab: BottomNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** Distinct icon per tab (Inbox = mail glyph, Search = magnifier, Compose = pencil,
 * Settings = gear) — never repeats an icon on the same screen per the design guideline. */
private val bottomNavEntries = listOf(
    BottomNavEntry(BottomNavTab.INBOX, "Inbox", Icons.Filled.Mail, Icons.Outlined.Mail),
    BottomNavEntry(BottomNavTab.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavEntry(BottomNavTab.COMPOSE, "Compose", Icons.Filled.Edit, Icons.Outlined.Edit),
    BottomNavEntry(BottomNavTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun MailBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
) {
    val colors = AppColors

    NavigationBar(containerColor = colors.card, contentColor = colors.textMuted) {
        bottomNavEntries.forEach { entry ->
            val selected = entry.tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(entry.tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
                        contentDescription = entry.label,
                    )
                },
                label = { Text(entry.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                    indicatorColor = colors.accentSoft,
                ),
            )
        }
    }
}
