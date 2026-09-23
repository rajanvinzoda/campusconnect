package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.data.model.UserRole

sealed class BottomNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    data object Feed : BottomNavDestination("feed", "Feed", Icons.Default.DynamicFeed, "bottom_nav_feed")
    data object Chat : BottomNavDestination("chat", "Chat", Icons.AutoMirrored.Filled.Chat, "bottom_nav_chat")
    data object Forums : BottomNavDestination("forums", "Forums", Icons.Default.Forum, "bottom_nav_forums")
    data object Career : BottomNavDestination("career", "Career Center", Icons.Default.Work, "bottom_nav_career")

    companion object {
        val items = listOf(Feed, Chat, Forums, Career)
    }
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.DynamicFeed),
    CHAT("Chat", Icons.AutoMirrored.Filled.Chat),
    FORUM("Forums", Icons.Default.Forum),
    EVENTS("Events", Icons.Default.Event),
    CAREER("Career", Icons.Default.Work),
    AI_STUDY("AI Study", Icons.Default.Psychology),
    RESOURCES("Resources", Icons.Default.Folder),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun RoleBadge(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(role.badgeColor).copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(role.badgeColor).copy(alpha = 0.5f))
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(role.badgeColor))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = role.displayName,
                color = Color(role.badgeColor),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusTopBar(
    currentRole: UserRole,
    onRoleSelected: ((UserRole) -> Unit)? = null,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotificationsCount: Int,
    onAdminDashboardClick: () -> Unit,
    onBackendSyncClick: () -> Unit,
    onNavigateRoute: ((String) -> Unit)? = null
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CampusConnect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Education Ecosystem",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // Verified Server-Assigned Role Badge (Read-only, no client-side role spoofing)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(currentRole.badgeColor).copy(alpha = 0.12f),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(currentRole.badgeColor).copy(alpha = 0.4f))
                ),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(currentRole.badgeColor))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentRole.displayName,
                        color = Color(currentRole.badgeColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Global Search")
            }

            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge { Text("$unreadNotificationsCount") }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }

            // More Sections Menu (Events, AI Study, Resources, Profile)
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.testTag("top_bar_more_menu_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Sections")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Events Hub") },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onNavigateRoute?.invoke("events")
                        },
                        modifier = Modifier.testTag("menu_item_events")
                    )
                    DropdownMenuItem(
                        text = { Text("AI Study Assistant") },
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onNavigateRoute?.invoke("ai_study")
                        },
                        modifier = Modifier.testTag("menu_item_ai_study")
                    )
                    DropdownMenuItem(
                        text = { Text("Resource Center") },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onNavigateRoute?.invoke("resources")
                        },
                        modifier = Modifier.testTag("menu_item_resources")
                    )
                    DropdownMenuItem(
                        text = { Text("My Profile") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        onClick = {
                            showMoreMenu = false
                            onNavigateRoute?.invoke("profile")
                        },
                        modifier = Modifier.testTag("menu_item_profile")
                    )
                }
            }

            // Only show Admin Center & Backend Operations for verified SUPER_ADMIN / Admin roles
            if (currentRole == UserRole.SUPER_ADMIN || currentRole == UserRole.UNIVERSITY_ADMIN) {
                IconButton(onClick = onAdminDashboardClick) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Center", tint = Color(0xFFDC2626))
                }
                IconButton(onClick = onBackendSyncClick) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Backend Operations", tint = Color(0xFF2563EB))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun CampusBottomNav(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier.testTag("campus_bottom_nav_bar")
    ) {
        BottomNavDestination.items.forEach { destination ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.testTag(destination.testTag),
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun CampusBottomNav(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
