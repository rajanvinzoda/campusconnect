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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onGenerateAiText: suspend (String) -> String,
    onGenerateFlashcards: suspend (String) -> List<Flashcard>,
    onGenerateQuiz: suspend (String) -> List<QuizQuestion>
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var promptInput by remember { mutableStateOf("") }
    var aiOutput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var quizQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var selectedOptionIndex by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Campus AI Academic Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tutor Explanations", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Flashcards", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Exam Quiz", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Ask an academic or engineering question...") },
                        placeholder = { Text("e.g. Explain Dijkstra's Algorithm with time complexity") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    aiOutput = onGenerateAiText(promptInput)
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && promptInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explain Concept")
                        }
                    }

                    if (aiOutput.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            LazyColumn(modifier = Modifier.padding(16.dp)) {
                                item {
                                    Text(
                                        text = aiOutput,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Enter subject for flashcards") },
                        placeholder = { Text("e.g. Operating Systems, Computer Networks") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    flashcards = onGenerateFlashcards(promptInput)
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && promptInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoading) "Generating Flashcards..." else "Generate Study Cards")
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(flashcards) { card ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Q: ${card.question}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "A: ${card.answer}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Enter topic for exam quiz") },
                        placeholder = { Text("e.g. Database Normalization, Software Testing") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    quizQuestions = onGenerateQuiz(promptInput)
                                    selectedOptionIndex = emptyMap()
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && promptInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoading) "Preparing Quiz..." else "Generate Practice Quiz")
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(quizQuestions.indices.toList()) { qIndex ->
                            val q = quizQuestions[qIndex]
                            val userChoice = selectedOptionIndex[qIndex]

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "${qIndex + 1}. ${q.question}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    q.options.forEachIndexed { optIndex, optText ->
                                        val isSelected = userChoice == optIndex
                                        val isCorrect = optIndex == q.correctIndex
                                        val chipColor = when {
                                            userChoice == null -> MaterialTheme.colorScheme.surface
                                            isSelected && isCorrect -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                            userChoice != null && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        Surface(
                                            onClick = {
                                                if (userChoice == null) {
                                                    selectedOptionIndex = selectedOptionIndex + (qIndex to optIndex)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = chipColor,
                                            border = CardDefaults.outlinedCardBorder(),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${('A'.code + optIndex).toChar()}) $optText",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                    if (userChoice != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Explanation: ${q.explanation}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
