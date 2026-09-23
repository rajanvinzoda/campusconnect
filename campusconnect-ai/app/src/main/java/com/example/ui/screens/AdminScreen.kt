package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.RoleBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    users: List<UserProfile>,
    onChangeRole: (userId: String, newRole: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: User Roles, 1: Moderation Queue, 2: Analytics

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "University Admin Panel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Role-Based Access Control & Content Moderation",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("User Roles") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Moderation") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Telemetry") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users, key = { it.id }) { u ->
                        var menuExpanded by remember { mutableStateOf(false) }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_user_card_${u.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(u.name, fontWeight = FontWeight.Bold)
                                    Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    RoleBadge(role = u.role)
                                }

                                Box {
                                    OutlinedButton(
                                        onClick = { menuExpanded = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Change Role")
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        UserRole.entries.filter { it != UserRole.SUPER_ADMIN }.forEach { r ->
                                            DropdownMenuItem(
                                                text = { RoleBadge(role = r) },
                                                onClick = {
                                                    onChangeRole(u.id, r)
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Moderation Queue Clean", fontWeight = FontWeight.Bold)
                        Text("All posts and files compliant with AI Safety Guidelines", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            2 -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Campus Network Telemetry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚡ Active Connected Users: 2,480", fontWeight = FontWeight.SemiBold)
                            Text("🛡️ Room DB Local Cache Sync: 100%", fontWeight = FontWeight.SemiBold)
                            Text("🤖 Gemini AI API Requests Today: 1,240", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
