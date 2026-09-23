package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampusEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    events: List<CampusEvent>,
    onRegisterEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQrModal by remember { mutableStateOf<CampusEvent?>(null) }
    var calendarSyncToast by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "University Calendar & Events",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Register for hackathons, workshops, & sports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        calendarSyncToast = "Synced 3 campus events to Google Calendar & iOS Calendar!"
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Sync Calendar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Calendar")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(events, key = { it.id }) { event ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_card_${event.id}")
                ) {
                    Column {
                        // Event Banner Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(event.bannerGradientStart),
                                            Color(event.bannerGradientEnd)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = event.category,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(event.date, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(event.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${event.attendeesCount} Registered", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Row {
                                    if (event.isRegistered) {
                                        OutlinedButton(
                                            onClick = { showQrModal = event },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("view_qr_attendance_button_${event.id}")
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = "QR Ticket")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("QR Ticket")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Button(
                                        onClick = { onRegisterEvent(event.id) },
                                        colors = if (event.isRegistered) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("register_event_button_${event.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (event.isRegistered) Icons.Default.Check else Icons.Default.EventAvailable,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (event.isRegistered) "Registered" else "Register Now")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // QR Attendance Modal Dialog
        showQrModal?.let { event ->
            AlertDialog(
                onDismissRequest = { showQrModal = null },
                title = { Text("QR Attendance Badge") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(180.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "QR Code",
                                        tint = Color.Black,
                                        modifier = Modifier.size(120.dp)
                                    )
                                    Text(
                                        text = event.qrCodeSeed.take(16),
                                        color = Color.DarkGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Scan at hall entrance for automated attendance & certificate generation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(onClick = { showQrModal = null }) {
                        Text("Close Ticket")
                    }
                }
            )
        }

        calendarSyncToast?.let { toast ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = { TextButton(onClick = { calendarSyncToast = null }) { Text("OK") } }
            ) {
                Text(toast)
            }
        }
    }
}
