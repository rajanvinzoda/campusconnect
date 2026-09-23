package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.PostEntity
import com.example.data.local.ResourceEntity
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CampusRepository(
    context: Context,
    private val syncRepository: BackendSyncRepository? = null
) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.campusDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Current logged in user state - Platform Owner & Super Admin
    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "usr_superadmin",
            name = "Vinzo Darajan",
            email = "vinzodarajan@gmail.com",
            role = UserRole.SUPER_ADMIN,
            department = "Computer Science & Engineering",
            branch = "AI & Distributed Systems",
            semester = 8,
            graduationYear = "2026",
            bio = "Platform Administrator & Researcher. Managing the CampusConnect ecosystem, AI study tooling, and distributed systems.",
            avatarUrl = null,
            coverUrl = null,
            skills = listOf("Kotlin", "Jetpack Compose", "TypeScript", "Prisma", "PostgreSQL", "Gemini API"),
            interests = listOf("Distributed Systems", "AI & Robotics", "Campus Innovation", "Open Source"),
            githubUrl = "https://github.com",
            linkedinUrl = "https://linkedin.com",
            websiteUrl = "https://campusconnect.edu",
            followersCount = 384,
            followingCount = 92,
            mutualConnections = 47,
            isVerified = true,
            certifications = listOf("Google Cloud Professional Architect", "Autonomous Systems Certification"),
            achievements = listOf("Platform Founder", "Dean's Research Fellowship"),
            clubs = listOf("AI & Robotics Society", "Hackathon Steering Committee")
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Profile creation state (defaults to true for instant interactive preview)
    private val _isProfileCreated = MutableStateFlow(true)
    val isProfileCreated: StateFlow<Boolean> = _isProfileCreated.asStateFlow()

    fun completeProfileSetup(
        name: String,
        email: String,
        role: UserRole,
        department: String,
        branch: String,
        bio: String,
        skills: List<String>,
        interests: List<String>
    ) {
        val updatedName = name.ifBlank { "Campus Member" }
        _currentUser.value = _currentUser.value.copy(
            name = updatedName,
            email = email.ifBlank { "student@university.edu" },
            role = role,
            department = department.ifBlank { "General Studies" },
            branch = branch.ifBlank { "1st Year" },
            bio = bio.ifBlank { "Welcome to my campus profile!" },
            skills = skills,
            interests = interests,
            isVerified = true
        )
        _isProfileCreated.value = true
        syncRepository?.logCloudSyncEvent("POST", "/v2/users/profile", "Profile for $updatedName created & synced to cloud")

        // Connect real-time Socket.IO connection on user profile setup
        val authPayload = com.example.network.SocketAuthPayload(
            token = "jwt_live_${System.currentTimeMillis()}",
            userId = _currentUser.value.id,
            userName = updatedName,
            role = role.name
        )
        syncRepository?.socketService?.connect(auth = authPayload)
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        syncRepository?.socketService?.let { socket ->
            scope.launch {
                socket.events.collect { event ->
                    when (event) {
                        is com.example.network.SocketEvent.NewMessage -> {
                            _activeMessages.value = _activeMessages.value + event.message
                        }
                        is com.example.network.SocketEvent.NewNotification -> {
                            _notifications.value = listOf(event.notification) + _notifications.value
                        }
                        is com.example.network.SocketEvent.NewPost -> {
                            _posts.value = listOf(event.post) + _posts.value
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun resetProfileSetup() {
        _isProfileCreated.value = false
    }

    // Security & Auth settings
    private val _biometricsEnabled = MutableStateFlow(false)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    // Feed state
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Forums state
    private val _forumTopics = MutableStateFlow<List<ForumTopic>>(emptyList())
    val forumTopics: StateFlow<List<ForumTopic>> = _forumTopics.asStateFlow()

    // Chat channels state
    private val _channels = MutableStateFlow<List<ChatChannel>>(emptyList())
    val channels: StateFlow<List<ChatChannel>> = _channels.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMessages: StateFlow<List<ChatMessage>> = _activeMessages.asStateFlow()

    // Events state
    private val _events = MutableStateFlow<List<CampusEvent>>(emptyList())
    val events: StateFlow<List<CampusEvent>> = _events.asStateFlow()

    // Resources state
    private val _resources = MutableStateFlow<List<AcademicResource>>(emptyList())
    val resources: StateFlow<List<AcademicResource>> = _resources.asStateFlow()

    // Career opportunities
    private val _careerItems = MutableStateFlow<List<CareerOpportunity>>(emptyList())
    val careerItems: StateFlow<List<CareerOpportunity>> = _careerItems.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Admin Users List for Role Management & Content Moderation
    private val _adminUserList = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUserList: StateFlow<List<UserProfile>> = _adminUserList.asStateFlow()

    init {
        seedInitialData()
        observeRoomDatabase()
    }

    private fun seedInitialData() {
        val initialPosts = listOf(
            Post(
                id = "post_001",
                authorId = "usr_admin_robert",
                authorName = "Dr. Robert Vance",
                authorRole = UserRole.UNIVERSITY_ADMIN,
                department = "Office of Academic Affairs",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
                type = PostType.ANNOUNCEMENT,
                content = "Welcome to the Fall 2026 Semester! All engineering labs and computational clusters are upgraded with high-performance nodes and Gemini developer sandboxes.",
                category = "Announcements",
                likesCount = 89,
                commentsCount = 14,
                sharesCount = 22,
                isLiked = false,
                isBookmarked = false,
                hashtags = listOf("CampusAnnouncements", "Semester2026", "AIResearch")
            ),
            Post(
                id = "post_002",
                authorId = "usr_superadmin",
                authorName = "Vinzo Darajan",
                authorRole = UserRole.SUPER_ADMIN,
                department = "Computer Science & Engineering",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
                type = PostType.EVENT,
                content = "🚀 CampusConnect Annual Hackathon 2026 Registration is now LIVE! 48 hours of building, $25,000 in grand prizes, and direct mentorship from Google and leading tech founders. Form your teams now!",
                category = "Events",
                likesCount = 142,
                commentsCount = 37,
                sharesCount = 58,
                isLiked = true,
                isBookmarked = true,
                hashtags = listOf("Hackathon2026", "BuildWithAI", "Innovation")
            ),
            Post(
                id = "post_003",
                authorId = "usr_elena_fac",
                authorName = "Prof. Elena Rostova",
                authorRole = UserRole.FACULTY,
                department = "Department of Artificial Intelligence",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 300,
                type = PostType.RESEARCH,
                content = "Our paper on 'Decentralized Microservices for Campus Information Networks' has been accepted at IEEE Cloud 2026! Looking for two undergraduate research assistants to help develop the demo prototype.",
                category = "Research",
                likesCount = 64,
                commentsCount = 9,
                sharesCount = 15,
                isLiked = false,
                isBookmarked = false,
                hashtags = listOf("IEEECloud", "ResearchGrant", "StudentOpportunity")
            ),
            Post(
                id = "post_004",
                authorId = "usr_maya_club",
                authorName = "Maya Patel",
                authorRole = UserRole.CLUB_LEADER,
                department = "ACM Student Chapter",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 480,
                type = PostType.POLL,
                content = "Which workshop track would you like our club to prioritize for next weekend's boot camp?",
                category = "Polls",
                pollOptions = listOf("Full-Stack Kotlin & Compose", "Autonomous Drone Navigation", "LLM Prompt Engineering & Agents", "Rust for Systems Programming"),
                pollVotes = mapOf(0 to 45, 1 to 28, 2 to 62, 3 to 19),
                likesCount = 97,
                commentsCount = 21,
                sharesCount = 11,
                isLiked = false,
                isBookmarked = false,
                hashtags = listOf("ACMSession", "StudentPoll", "SkillUp")
            )
        )

        _posts.value = initialPosts

        _channels.value = listOf(
            ChatChannel(
                id = "chan_general",
                name = "#general-campus",
                isGroup = true,
                lastMessage = "Hackathon project guidelines have been posted to the hub!",
                lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 5,
                unreadCount = 2,
                isOnline = true,
                category = "University Wide"
            ),
            ChatChannel(
                id = "chan_hackathon",
                name = "#hackathon-2026",
                isGroup = true,
                lastMessage = "Anyone looking for a backend developer proficient in Prisma & Node?",
                lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 18,
                unreadCount = 0,
                isOnline = true,
                category = "Clubs & Events"
            ),
            ChatChannel(
                id = "chan_ai_lab",
                name = "#ai-robotics-research",
                isGroup = true,
                lastMessage = "Meeting scheduled at 4 PM in Lab 304.",
                lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 42,
                unreadCount = 0,
                isOnline = false,
                category = "Academic Labs"
            ),
            ChatChannel(
                id = "chan_dr_vance",
                name = "Dr. Robert Vance (Dean)",
                isGroup = false,
                lastMessage = "Please review the updated semester system policy draft.",
                lastMessageTime = System.currentTimeMillis() - 1000 * 60 * 95,
                unreadCount = 1,
                isOnline = true,
                category = "Direct Messages"
            )
        )

        _activeMessages.value = listOf(
            ChatMessage(
                id = "msg_001",
                channelId = "chan_general",
                senderId = "usr_alex_stud",
                senderName = "Alex Rivera",
                text = "Hey everyone! When does the registration for Hackathon 2026 close?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 25
            ),
            ChatMessage(
                id = "msg_002",
                channelId = "chan_general",
                senderId = "usr_superadmin",
                senderName = "Vinzo Darajan",
                text = "Registration closes this Friday at 11:59 PM. Make sure your teams have at least 2 members registered!",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 15
            ),
            ChatMessage(
                id = "msg_003",
                channelId = "chan_general",
                senderId = "usr_elena_fac",
                senderName = "Prof. Elena Rostova",
                text = "Faculty mentors will also be available on Saturday morning to help with architecture design.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 5
            )
        )

        _forumTopics.value = listOf(
            ForumTopic(
                id = "forum_001",
                title = "Best architectural practices for low-latency WebSocket clustering?",
                authorName = "Alex Rivera",
                authorRole = UserRole.STUDENT,
                category = "Distributed Systems",
                content = "We are designing a real-time multiplayer board for campus clubs. What are the recommended Redis adapter patterns to ensure horizontal scalability?",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                upvotes = 34,
                downvotes = 1,
                userVote = 1,
                repliesCount = 8,
                tags = listOf("WebSockets", "Redis", "NodeJS", "Architecture"),
                isSolved = true,
                bestAnswer = "Use Socket.IO Redis Streams Adapter combined with sticky sessions on NGINX upstream."
            ),
            ForumTopic(
                id = "forum_002",
                title = "How to prepare for Google & NVIDIA campus placement coding rounds?",
                authorName = "Maya Patel",
                authorRole = UserRole.CLUB_LEADER,
                category = "Career & Placements",
                content = "Sharing study tracks and curated LeetCode problem lists for dynamic programming and graph algorithms.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 360,
                upvotes = 52,
                downvotes = 0,
                userVote = 0,
                repliesCount = 14,
                tags = listOf("Placements", "DSA", "InterviewPrep"),
                isSolved = false
            )
        )

        _events.value = listOf(
            CampusEvent(
                id = "evt_001",
                title = "CampusConnect Hackathon 2026",
                organizer = "ACM & IEEE Student Chapters",
                category = "Hackathon",
                date = "Oct 12 - 14, 2026 • 48 Hours",
                location = "Grand Auditorium & Innovation Hub",
                description = "Build cutting-edge AI and mobile apps solving real campus and societal challenges. Industry mentors, free food, and $25k prize pool.",
                attendeesCount = 340,
                isRegistered = true,
                qrCodeSeed = "CAMPUS_HACK_2026_SEED_VINZO",
                bannerGradientStart = 0xFF4338CA,
                bannerGradientEnd = 0xFF6D28D9
            ),
            CampusEvent(
                id = "evt_002",
                title = "Annual Career Fair & Tech Expo",
                organizer = "University Placement Cell",
                category = "Career Fair",
                date = "Nov 3, 2026 • 9:00 AM - 5:00 PM",
                location = "Campus Sports Complex",
                description = "Meet recruiters and alumni from top tech, consulting, and research organizations. On-the-spot interviews and resume evaluations.",
                attendeesCount = 820,
                isRegistered = false,
                qrCodeSeed = "CAMPUS_CAREER_2026",
                bannerGradientStart = 0xFF0D9488,
                bannerGradientEnd = 0xFF059669
            ),
            CampusEvent(
                id = "evt_003",
                title = "AI & Autonomous Systems Guest Lecture",
                organizer = "Department of CSE",
                category = "Guest Lecture",
                date = "Oct 22, 2026 • 2:00 PM",
                location = "Seminar Hall 201",
                description = "Keynote address on Generative AI agent architectures and edge deployment strategies.",
                attendeesCount = 180,
                isRegistered = false,
                qrCodeSeed = "CAMPUS_AI_LECTURE",
                bannerGradientStart = 0xFF2563EB,
                bannerGradientEnd = 0xFF1D4ED8
            )
        )

        _resources.value = listOf(
            AcademicResource(
                id = "res_001",
                title = "Distributed Systems & Cloud Computing Lecture Notes (CS401)",
                subject = "Computer Science",
                department = "CSE",
                fileType = "PDF",
                authorName = "Prof. Elena Rostova",
                rating = 4.9f,
                downloadsCount = 512,
                isDownloaded = true,
                tags = listOf("Cloud", "Consensus", "Raft", "Distributed")
            ),
            AcademicResource(
                id = "res_002",
                title = "Comprehensive Data Structures & Algorithms Lab Manual",
                subject = "Algorithms",
                department = "CSE",
                fileType = "CODE",
                authorName = "Dr. Robert Vance",
                rating = 4.8f,
                downloadsCount = 428,
                isDownloaded = false,
                tags = listOf("DSA", "Graphs", "Trees", "Sorting")
            ),
            AcademicResource(
                id = "res_003",
                title = "Machine Learning & Neural Networks Midterm Previous Question Papers",
                subject = "Artificial Intelligence",
                department = "AI & DS",
                fileType = "EXAM_PAPER",
                authorName = "Academic Affairs",
                rating = 4.7f,
                downloadsCount = 680,
                isDownloaded = false,
                tags = listOf("Exams", "Midterm", "SolvedPapers")
            )
        )

        _careerItems.value = listOf(
            CareerOpportunity(
                id = "job_001",
                companyName = "Google",
                roleTitle = "Software Engineering Intern - Android & Cloud",
                type = "Internship",
                location = "Mountain View, CA / Hybrid",
                stipend = "$9,200 / month",
                deadline = "Rolling Applications",
                requiredSkills = listOf("Kotlin", "Jetpack Compose", "Coroutines", "System Design"),
                description = "Work with world-class engineering teams building modern mobile and cloud platforms used by billions of users.",
                isApplied = true
            ),
            CareerOpportunity(
                id = "job_002",
                companyName = "NVIDIA",
                roleTitle = "Deep Learning Systems Research Intern",
                type = "Internship",
                location = "Santa Clara, CA / Remote",
                stipend = "$8,800 / month",
                deadline = "Oct 30, 2026",
                requiredSkills = listOf("Python", "PyTorch", "CUDA", "C++"),
                description = "Accelerate next-generation AI model inference pipelines and edge robotics algorithms.",
                isApplied = false
            ),
            CareerOpportunity(
                id = "job_003",
                companyName = "CampusConnect Engineering Lab",
                roleTitle = "Graduate Research Assistant - Distributed Architecture",
                type = "Research Assistant",
                location = "On-Campus (Research Park)",
                stipend = "$3,500 / month",
                deadline = "Nov 15, 2026",
                requiredSkills = listOf("TypeScript", "PostgreSQL", "Docker", "Socket.IO"),
                description = "Maintain and enhance the core campus education ecosystem infrastructure.",
                isApplied = false
            )
        )

        _notifications.value = listOf(
            NotificationItem(
                id = "notif_001",
                title = "Welcome Super Admin!",
                message = "You have full platform privileges for CampusConnect AI.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 10,
                type = "ANNOUNCEMENT",
                isRead = false
            ),
            NotificationItem(
                id = "notif_002",
                title = "Hackathon 2026 Registration",
                message = "Your registration for CampusConnect Hackathon 2026 has been confirmed.",
                timestamp = System.currentTimeMillis() - 1000 * 60 * 60,
                type = "EVENT",
                isRead = true
            )
        )

        _adminUserList.value = listOf(
            _currentUser.value,
            UserProfile(
                id = "usr_admin_robert",
                name = "Dr. Robert Vance",
                email = "robert.vance@university.edu",
                role = UserRole.UNIVERSITY_ADMIN,
                department = "Office of the Dean",
                branch = "Academic Affairs",
                semester = 0,
                graduationYear = "Faculty",
                bio = "Dean of Engineering and Platform Coordinator.",
                isVerified = true
            ),
            UserProfile(
                id = "usr_elena_fac",
                name = "Prof. Elena Rostova",
                email = "elena.rostova@university.edu",
                role = UserRole.FACULTY,
                department = "Computer Science",
                branch = "Artificial Intelligence",
                semester = 0,
                graduationYear = "Faculty",
                bio = "Associate Professor in Distributed Computing & Machine Learning.",
                isVerified = true
            ),
            UserProfile(
                id = "usr_alex_stud",
                name = "Alex Rivera",
                email = "alex.rivera@campusconnect.edu",
                role = UserRole.STUDENT,
                department = "Computer Science",
                branch = "Software Engineering",
                semester = 6,
                graduationYear = "2027",
                bio = "Junior student passionate about mobile engineering.",
                isVerified = true
            ),
            UserProfile(
                id = "usr_maya_club",
                name = "Maya Patel",
                email = "maya.patel@campusconnect.edu",
                role = UserRole.CLUB_LEADER,
                department = "Information Technology",
                branch = "Network Security",
                semester = 7,
                graduationYear = "2026",
                bio = "ACM Student Chapter President & Hackathon Organizer.",
                isVerified = true
            )
        )

        // Sync initial posts into Room database
        scope.launch {
            val entities = initialPosts.map { p ->
                PostEntity(
                    id = p.id,
                    authorName = p.authorName,
                    authorRole = p.authorRole.name,
                    department = p.department,
                    timestamp = p.timestamp,
                    type = p.type.name,
                    content = p.content,
                    likesCount = p.likesCount,
                    commentsCount = p.commentsCount,
                    isLiked = p.isLiked,
                    isBookmarked = p.isBookmarked,
                    category = p.category
                )
            }
            dao.insertPosts(entities)
        }
    }

    private fun observeRoomDatabase() {
        scope.launch {
            dao.getAllPosts().collect { roomPosts ->
                if (roomPosts.isNotEmpty()) {
                    // Sync Room changes with local post state
                    val updatedPosts = _posts.value.map { currentPost ->
                        val cached = roomPosts.find { it.id == currentPost.id }
                        if (cached != null) {
                            currentPost.copy(
                                likesCount = cached.likesCount,
                                isLiked = cached.isLiked,
                                isBookmarked = cached.isBookmarked
                            )
                        } else currentPost
                    }
                    _posts.value = updatedPosts
                }
            }
        }
    }

    // Role Switching
    fun updateUserRole(role: UserRole) {
        _currentUser.value = _currentUser.value.copy(role = role)
        syncRepository?.logCloudSyncEvent("PATCH", "/v2/users/role", "Role updated to ${role.displayName}")
    }

    fun updateProfileInfo(name: String, department: String, branch: String, bio: String, skills: List<String>, interests: List<String>) {
        _currentUser.value = _currentUser.value.copy(
            name = name,
            department = department,
            branch = branch,
            bio = bio,
            skills = skills,
            interests = interests
        )
        syncRepository?.logCloudSyncEvent("PUT", "/v2/users/profile", "Profile details updated for $name")
    }

    fun toggleBiometrics(enabled: Boolean) {
        _biometricsEnabled.value = enabled
    }

    fun toggleTwoFactor(enabled: Boolean) {
        _twoFactorEnabled.value = enabled
    }

    // Post Interactions
    fun createPost(content: String, type: PostType, category: String, hashtags: List<String>) {
        val user = _currentUser.value
        val newPost = Post(
            id = "post_${System.currentTimeMillis()}",
            authorId = user.id,
            authorName = user.name,
            authorRole = user.role,
            department = user.department,
            timestamp = System.currentTimeMillis(),
            type = type,
            content = content,
            category = category,
            hashtags = hashtags
        )
        _posts.value = listOf(newPost) + _posts.value

        syncRepository?.logCloudSyncEvent("POST", "/v2/posts/create", "Live post by ${user.name} dispatched to cloud")

        val postJson = org.json.JSONObject().apply {
            put("id", newPost.id)
            put("authorId", newPost.authorId)
            put("authorName", newPost.authorName)
            put("content", newPost.content)
            put("type", newPost.type.name)
            put("timestamp", newPost.timestamp)
        }
        syncRepository?.socketService?.emit("post:create", postJson)

        scope.launch {
            dao.insertPosts(listOf(
                PostEntity(
                    id = newPost.id,
                    authorName = newPost.authorName,
                    authorRole = newPost.authorRole.name,
                    department = newPost.department,
                    timestamp = newPost.timestamp,
                    type = newPost.type.name,
                    content = newPost.content,
                    likesCount = 0,
                    commentsCount = 0,
                    isLiked = false,
                    isBookmarked = false,
                    category = newPost.category
                )
            ))
        }
    }

    fun toggleLikePost(postId: String) {
        val list = _posts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likesCount + 1 else post.likesCount - 1
                scope.launch {
                    dao.updatePostLike(postId, newLiked, if (newLiked) 1 else -1)
                }
                post.copy(isLiked = newLiked, likesCount = newCount)
            } else post
        }
        _posts.value = list
    }

    fun toggleBookmarkPost(postId: String) {
        val list = _posts.value.map { post ->
            if (post.id == postId) {
                val newBM = !post.isBookmarked
                scope.launch {
                    dao.updatePostBookmark(postId, newBM)
                }
                post.copy(isBookmarked = newBM)
            } else post
        }
        _posts.value = list
    }

    // Forum Actions
    fun voteForumTopic(topicId: String, delta: Int) {
        val updated = _forumTopics.value.map { topic ->
            if (topic.id == topicId) {
                val newVote = if (topic.userVote == delta) 0 else delta
                val voteDiff = newVote - topic.userVote
                topic.copy(
                    userVote = newVote,
                    upvotes = if (voteDiff > 0) topic.upvotes + voteDiff else topic.upvotes,
                    downvotes = if (voteDiff < 0) topic.downvotes - voteDiff else topic.downvotes
                )
            } else topic
        }
        _forumTopics.value = updated
    }

    fun createForumTopic(title: String, category: String, content: String, tags: List<String>) {
        val user = _currentUser.value
        val newTopic = ForumTopic(
            id = "forum_${System.currentTimeMillis()}",
            title = title,
            authorName = user.name,
            authorRole = user.role,
            category = category,
            content = content,
            timestamp = System.currentTimeMillis(),
            tags = tags
        )
        _forumTopics.value = listOf(newTopic) + _forumTopics.value
        syncRepository?.logCloudSyncEvent("POST", "/v2/forums/topics", "Forum discussion '$title' posted to cloud")
    }

    // Chat Actions
    fun sendMessage(channelId: String, text: String, isVoiceNote: Boolean = false, durationSec: Int = 0) {
        val user = _currentUser.value
        val msg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            channelId = channelId,
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            isVoiceNote = isVoiceNote,
            voiceDurationSec = durationSec
        )
        _activeMessages.value = _activeMessages.value + msg
        syncRepository?.logCloudSyncEvent("POST", "/v2/chat/messages", "Live chat message dispatched by ${user.name}")

        val msgJson = org.json.JSONObject().apply {
            put("id", msg.id)
            put("channelId", msg.channelId)
            put("senderId", msg.senderId)
            put("senderName", msg.senderName)
            put("text", msg.text)
            put("timestamp", msg.timestamp)
        }
        syncRepository?.socketService?.emit("chat:message", msgJson)

        scope.launch {
            dao.insertMessage(
                ChatMessageEntity(
                    id = msg.id,
                    channelId = msg.channelId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    text = msg.text,
                    timestamp = msg.timestamp,
                    isVoiceNote = msg.isVoiceNote,
                    voiceDurationSec = msg.voiceDurationSec,
                    attachmentUrl = msg.attachmentUrl
                )
            )
        }
    }

    // Events Actions
    fun toggleRegisterEvent(eventId: String) {
        val updated = _events.value.map { evt ->
            if (evt.id == eventId) {
                val newReg = !evt.isRegistered
                val newCount = if (newReg) evt.attendeesCount + 1 else evt.attendeesCount - 1
                syncRepository?.logCloudSyncEvent("POST", "/v2/events/register", "Event registration state changed for '${evt.title}'")
                evt.copy(isRegistered = newReg, attendeesCount = newCount)
            } else evt
        }
        _events.value = updated
    }

    // Resource Actions
    fun toggleDownloadResource(resourceId: String) {
        val updated = _resources.value.map { res ->
            if (res.id == resourceId) {
                val newDown = !res.isDownloaded
                val newCount = if (newDown) res.downloadsCount + 1 else res.downloadsCount
                syncRepository?.logCloudSyncEvent("GET", "/v2/resources/download", "Academic resource '${res.title}' requested")
                scope.launch {
                    dao.markResourceDownloaded(resourceId, newDown)
                }
                res.copy(isDownloaded = newDown, downloadsCount = newCount)
            } else res
        }
        _resources.value = updated
    }

    fun applyToCareer(jobId: String) {
        val updated = _careerItems.value.map { job ->
            if (job.id == jobId) {
                syncRepository?.logCloudSyncEvent("POST", "/v2/career/apply", "Job application submitted for '${job.roleTitle}'")
                job.copy(isApplied = true)
            } else job
        }
        _careerItems.value = updated
    }

    // Admin Actions
    fun changeUserRoleByAdmin(userId: String, newRole: UserRole) {
        val updated = _adminUserList.value.map { u ->
            if (u.id == userId) u.copy(role = newRole) else u
        }
        _adminUserList.value = updated
        if (userId == _currentUser.value.id) {
            _currentUser.value = _currentUser.value.copy(role = newRole)
        }
    }
}
