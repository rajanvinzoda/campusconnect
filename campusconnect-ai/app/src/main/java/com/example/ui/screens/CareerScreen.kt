package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.CareerOpportunity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerScreen(
    careerItems: List<CareerOpportunity>,
    onApplyToCareer: (String) -> Unit,
    onAnalyzeResumeWithAi: suspend (String) -> String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Placement Drive, 1: AI Resume Reviewer, 2: Alumni Mentorship
    var resumeInputText by remember { mutableStateOf("Alex Rivera • CS Senior\nSkills: Kotlin, Jetpack Compose, Python, Gemini API, System Architecture, PostgreSQL.\nProjects: Built CampusConnect AI social mobile ecosystem, High-performance Edge Computing research.\nExperience: Software Intern at Campus Tech Lab.") }
    var aiAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Placement Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Work, contentDescription = null) },
                modifier = Modifier.testTag("career_tab_jobs")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("AI Resume Review", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                modifier = Modifier.testTag("career_tab_resume")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Alumni Mentors", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                modifier = Modifier.testTag("career_tab_mentors")
            )
        }

        when (selectedTab) {
            0 -> {
                // Job Opportunities List
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(careerItems, key = { it.id }) { job ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("job_card_${job.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(job.roleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${job.companyName} • ${job.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(job.type, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(job.stipend, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Deadline: ${job.deadline}", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(job.description, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    job.requiredSkills.forEach { skill ->
                                        SuggestionChip(onClick = { }, label = { Text(skill, fontSize = 10.sp) })
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Button(
                                    onClick = { onApplyToCareer(job.id) },
                                    enabled = !job.isApplied,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("apply_job_button_${job.id}")
                                ) {
                                    Icon(
                                        imageVector = if (job.isApplied) Icons.Default.Check else Icons.Default.Send,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (job.isApplied) "Application Submitted" else "1-Click Apply with University Profile")
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // AI Resume Reviewer Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Gemini AI Resume Analyzer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paste your resume summary or technical bio to get instant feedback & score for campus placements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = resumeInputText,
                        onValueChange = { resumeInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("resume_input_text")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isAnalyzing = true
                            coroutineScope.launch {
                                val result = onAnalyzeResumeWithAi(resumeInputText)
                                aiAnalysisResult = result
                                isAnalyzing = false
                            }
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_resume_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing with Gemini AI...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Review Resume with Gemini AI")
                        }
                    }

                    aiAnalysisResult?.let { res ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini Placement Score & Feedback", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                Text(res, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
            2 -> {
                // Alumni Mentors Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("1-on-1 Alumni Mentorship Network", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        Triple("Sarah Jenkins", "SDE-II at Google • Class of 2024", "System Design, LeetCode, Distributed Systems"),
                        Triple("David Kim", "Software Engineer at Apple • Class of 2025", "iOS, Swift, Edge AI, Interview Prep"),
                        Triple("Rohan Gupta", "AI Research Scientist at NVIDIA • Class of 2023", "PyTorch, CUDA, Computer Vision")
                    ).forEach { mentor ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mentor.first, fontWeight = FontWeight.Bold)
                                    Text(mentor.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    Text(mentor.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Book 1:1")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
