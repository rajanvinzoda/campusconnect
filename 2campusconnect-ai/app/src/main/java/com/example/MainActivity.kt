package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.BackendSyncRepository
import com.example.data.repository.CampusRepository
import com.example.data.repository.GeminiRepository
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.CampusBottomNav
import com.example.ui.components.CampusTopBar
import com.example.ui.components.NavigationTab
import com.example.ui.screens.*
import com.example.ui.theme.CampusConnectTheme

class MainActivity : ComponentActivity() {

    private lateinit var campusRepository: CampusRepository
    private lateinit var geminiRepository: GeminiRepository
    private lateinit var backendSyncRepository: BackendSyncRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        backendSyncRepository = BackendSyncRepository(applicationContext)
        campusRepository = CampusRepository(applicationContext, backendSyncRepository)
        geminiRepository = GeminiRepository()

        setContent {
            CampusConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppScreen(campusRepository, geminiRepository, backendSyncRepository)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    campusRepository: CampusRepository,
    geminiRepository: GeminiRepository,
    backendSyncRepository: BackendSyncRepository
) {
    val navController = rememberNavController()
    var showAuthOverlay by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var showNotificationsOverlay by remember { mutableStateOf(false) }
    var showAdminOverlay by remember { mutableStateOf(false) }
    var showBackendSyncOverlay by remember { mutableStateOf(false) }

    // Collect repository state flows
    val currentUser by campusRepository.currentUser.collectAsState()
    val isProfileCreated by campusRepository.isProfileCreated.collectAsState()
    val biometricsEnabled by campusRepository.biometricsEnabled.collectAsState()
    val twoFactorEnabled by campusRepository.twoFactorEnabled.collectAsState()

    val posts by campusRepository.posts.collectAsState()
    val forumTopics by campusRepository.forumTopics.collectAsState()
    val channels by campusRepository.channels.collectAsState()
    val activeMessages by campusRepository.activeMessages.collectAsState()
    val events by campusRepository.events.collectAsState()
    val resources by campusRepository.resources.collectAsState()
    val careerItems by campusRepository.careerItems.collectAsState()
    val notifications by campusRepository.notifications.collectAsState()
    val adminUserList by campusRepository.adminUserList.collectAsState()

    Scaffold(
        topBar = {
            if (isProfileCreated && !showSearchOverlay && !showNotificationsOverlay && !showAdminOverlay && !showAuthOverlay && !showBackendSyncOverlay) {
                CampusTopBar(
                    currentRole = currentUser.role,
                    onRoleSelected = { role -> campusRepository.updateUserRole(role) },
                    onSearchClick = { showSearchOverlay = true },
                    onNotificationsClick = { showNotificationsOverlay = true },
                    unreadNotificationsCount = notifications.size,
                    onAdminDashboardClick = { showAdminOverlay = true },
                    onBackendSyncClick = { showBackendSyncOverlay = true },
                    onNavigateRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isProfileCreated && !showSearchOverlay && !showNotificationsOverlay && !showAdminOverlay && !showAuthOverlay && !showBackendSyncOverlay) {
                CampusBottomNav(navController = navController)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isProfileCreated) innerPadding else PaddingValues(0.dp))
        ) {
            when {
                !isProfileCreated -> {
                    OnboardingProfileScreen(
                        onCompleteProfile = { name, email, role, dept, branch, bio, skills, interests ->
                            campusRepository.completeProfileSetup(
                                name = name,
                                email = email,
                                role = role,
                                department = dept,
                                branch = branch,
                                bio = bio,
                                skills = skills,
                                interests = interests
                            )
                        }
                    )
                }
                showAuthOverlay -> {
                    AuthScreen(
                        currentRole = currentUser.role,
                        onRoleSelected = { campusRepository.updateUserRole(it) },
                        biometricsEnabled = biometricsEnabled,
                        onToggleBiometrics = { campusRepository.toggleBiometrics(it) },
                        twoFactorEnabled = twoFactorEnabled,
                        onToggleTwoFactor = { campusRepository.toggleTwoFactor(it) },
                        onLoginSuccess = { showAuthOverlay = false }
                    )
                }
                showSearchOverlay -> {
                    GlobalSearchScreen(
                        posts = posts,
                        resources = resources,
                        events = events,
                        onClose = { showSearchOverlay = false }
                    )
                }
                showNotificationsOverlay -> {
                    NotificationsScreen(
                        notifications = notifications,
                        onClose = { showNotificationsOverlay = false }
                    )
                }
                showAdminOverlay -> {
                    AdminScreen(
                        users = adminUserList,
                        onChangeRole = { uId, role -> campusRepository.changeUserRoleByAdmin(uId, role) }
                    )
                }
                showBackendSyncOverlay -> {
                    BackendSyncScreen(
                        syncRepository = backendSyncRepository,
                        onClose = { showBackendSyncOverlay = false }
                    )
                }
                else -> {
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavDestination.Feed.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(BottomNavDestination.Feed.route) {
                            FeedScreen(
                                posts = posts,
                                onCreatePost = { content, type, category, hashtags ->
                                    campusRepository.createPost(content, type, category, hashtags)
                                },
                                onLikePost = { postId -> campusRepository.toggleLikePost(postId) },
                                onBookmarkPost = { postId -> campusRepository.toggleBookmarkPost(postId) },
                                currentRole = currentUser.role
                            )
                        }
                        composable(BottomNavDestination.Chat.route) {
                            ChatScreen(
                                channels = channels,
                                messages = activeMessages,
                                onSendMessage = { channelId, text, isVoice, duration ->
                                    campusRepository.sendMessage(channelId, text, isVoice, duration)
                                }
                            )
                        }
                        composable(BottomNavDestination.Forums.route) {
                            ForumScreen(
                                topics = forumTopics,
                                onVoteTopic = { tId, delta -> campusRepository.voteForumTopic(tId, delta) },
                                onCreateTopic = { title, cat, content, tags ->
                                    campusRepository.createForumTopic(title, cat, content, tags)
                                }
                            )
                        }
                        composable(BottomNavDestination.Career.route) {
                            CareerScreen(
                                careerItems = careerItems,
                                onApplyToCareer = { jobId -> campusRepository.applyToCareer(jobId) },
                                onAnalyzeResumeWithAi = { resumeText ->
                                    geminiRepository.analyzeResume(resumeText)
                                }
                            )
                        }
                        composable("events") {
                            EventsScreen(
                                events = events,
                                onRegisterEvent = { eventId -> campusRepository.toggleRegisterEvent(eventId) }
                            )
                        }
                        composable("ai_study") {
                            AiAssistantScreen(
                                onGenerateAiText = { prompt -> geminiRepository.generateAcademicExplanation(prompt) },
                                onGenerateFlashcards = { subject -> geminiRepository.generateFlashcards(subject) },
                                onGenerateQuiz = { topic -> geminiRepository.generateQuiz(topic) }
                            )
                        }
                        composable("resources") {
                            ResourceHubScreen(
                                resources = resources,
                                onDownloadResource = { resId -> campusRepository.toggleDownloadResource(resId) }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                user = currentUser,
                                onUpdateProfile = { name, department, branch, bio, skills, interests ->
                                    campusRepository.updateProfileInfo(name, department, branch, bio, skills, interests)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    CampusConnectTheme {
        Surface {
            Text("CampusConnect AI System Ready")
        }
    }
}
