package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.CodingProject
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.LonaRepository
import com.example.ui.components.EdgeLightFrame
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodingWorkspaceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LonaAiTheme
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.configureAppCheck
import com.example.voice.AssistantVoiceState
import com.example.voice.VoiceAssistantManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MainNavTab {
    HOME,
    CHAT,
    WORKSPACE,
    TOOLS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: App Check contract - configure before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            LonaAppRoot()
        }
    }
}

@Composable
fun LonaAppRoot() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }

    // Repositories & Managers
    val lonaRepository = remember { LonaRepository(context) }
    val aiRepository = remember { AiAssistantRepository() }
    val voiceManager = remember { VoiceAssistantManager(context, coroutineScope) }

    // Listen for Auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
            voiceManager.release()
        }
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
    } else if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onContinueGuest = { isGuestMode = true }
        )
    } else {
        MainFeatureApp(
            userId = currentUser?.uid ?: "guest_user",
            userEmail = currentUser?.email ?: "guest@lona.ai",
            userDisplayName = currentUser?.displayName ?: "Guest Explorer",
            lonaRepository = lonaRepository,
            aiRepository = aiRepository,
            voiceManager = voiceManager,
            onSignOut = {
                isGuestMode = false
                val credManager = androidx.credentials.CredentialManager.create(context)
                signOutUser(context, credManager, { currentUser = null }, coroutineScope)
            }
        )
    }
}

@Composable
fun MainFeatureApp(
    userId: String,
    userEmail: String,
    userDisplayName: String,
    lonaRepository: LonaRepository,
    aiRepository: AiAssistantRepository,
    voiceManager: VoiceAssistantManager,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var accentColor by remember { mutableStateOf(NeonPurple) }
    var edgeLightEnabled by remember { mutableStateOf(true) }
    var selectedModel by remember { mutableStateOf("gemini-3.8-flash") }

    // Voice States
    val voiceState by voiceManager.voiceState.collectAsState()
    val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
    val statusMessage by voiceManager.statusMessage.collectAsState()

    // Conversations & Messages State
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var currentConversationId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingText by remember { mutableStateOf("") }
    var chatPrefilledPrompt by remember { mutableStateOf("") }

    // Coding Workspace State
    var currentProject by remember {
        mutableStateOf(
            CodingProject(
                name = "Cyberpunk Login",
                templateType = "login",
                htmlCode = defaultLoginHtml,
                cssCode = defaultLoginCss,
                jsCode = "console.log('LONA Workspace Active');"
            )
        )
    }
    var isWorkspaceGenerating by remember { mutableStateOf(false) }
    var workspaceGenerationStatus by remember { mutableStateOf("") }

    // Permission Launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceManager.startListening { queryText ->
                // Voice query recognized! Route to AI Assistant
                coroutineScope.launch {
                    val activeConvId = currentConversationId ?: run {
                        val newId = lonaRepository.createConversation(
                            title = queryText.take(30),
                            model = selectedModel
                        )
                        currentConversationId = newId
                        newId
                    }

                    // Add user message to Firestore
                    lonaRepository.addMessage(activeConvId, "user", queryText)

                    // Get AI response
                    val aiResult = aiRepository.generateResponse(queryText, selectedModel)
                    val responseText = aiResult.getOrDefault("I processed your query: $queryText")

                    lonaRepository.addMessage(activeConvId, "assistant", responseText)

                    // Synthesize spoken answer
                    voiceManager.speak(responseText)
                }
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice interaction", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to toggle microphone with permission check
    val toggleVoiceAction: () -> Unit = {
        if (voiceState == AssistantVoiceState.LISTENING) {
            voiceManager.stopListening()
        } else if (voiceState == AssistantVoiceState.SPEAKING) {
            voiceManager.stopSpeaking()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                voiceManager.startListening { queryText ->
                    coroutineScope.launch {
                        val activeConvId = currentConversationId ?: run {
                            val newId = lonaRepository.createConversation(title = queryText.take(30), model = selectedModel)
                            currentConversationId = newId
                            newId
                        }
                        lonaRepository.addMessage(activeConvId, "user", queryText)
                        val aiResult = aiRepository.generateResponse(queryText, selectedModel)
                        val responseText = aiResult.getOrDefault("Processed query.")
                        lonaRepository.addMessage(activeConvId, "assistant", responseText)
                        voiceManager.speak(responseText)
                    }
                }
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Observe conversations
    LaunchedEffect(userId) {
        if (userId != "guest_user") {
            try {
                lonaRepository.observeConversations().collectLatest { convs ->
                    conversations = convs
                    if (currentConversationId == null && convs.isNotEmpty()) {
                        currentConversationId = convs.first().id
                    }
                }
            } catch (e: Exception) {
                // Handled in repository via handleFirestoreError
            }
        }
    }

    // Observe messages when current conversation changes
    LaunchedEffect(currentConversationId) {
        val convId = currentConversationId
        if (convId != null && userId != "guest_user") {
            try {
                lonaRepository.observeMessages(convId).collectLatest { msgs ->
                    messages = msgs
                }
            } catch (e: Exception) {
                // Handled in repository
            }
        }
    }

    // BackHandler: When in a secondary tab, pressing Back returns to HOME
    BackHandler(enabled = currentTab != MainNavTab.HOME) {
        currentTab = MainNavTab.HOME
    }

    LonaAiTheme(accentColor = accentColor) {
        EdgeLightFrame(enabled = edgeLightEnabled) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = CyberBackground,
                bottomBar = {
                    LonaBottomNavigationBar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onCenterMicClicked = toggleVoiceAction,
                        isListening = voiceState == AssistantVoiceState.LISTENING
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainNavTab.HOME -> HomeScreen(
                            voiceState = voiceState,
                            audioAmplitude = audioAmplitude,
                            statusMessage = statusMessage,
                            selectedModel = selectedModel,
                            onToggleVoice = toggleVoiceAction,
                            onNavigateToChatWithPrompt = { prompt ->
                                chatPrefilledPrompt = prompt
                                currentTab = MainNavTab.CHAT
                            },
                            onNavigateToWorkspaceWithTemplate = { template ->
                                applyTemplate(template) { currentProject = it }
                                currentTab = MainNavTab.WORKSPACE
                            },
                            onNavigateToSettings = { currentTab = MainNavTab.SETTINGS }
                        )

                        MainNavTab.CHAT -> ChatScreen(
                            messages = messages,
                            isStreaming = isStreaming,
                            streamingText = streamingText,
                            selectedModel = selectedModel,
                            onModelSelected = { selectedModel = it },
                            prefilledPrompt = chatPrefilledPrompt,
                            onSendMessage = { promptText ->
                                coroutineScope.launch {
                                    val activeConvId = currentConversationId ?: run {
                                        val newId = lonaRepository.createConversation(
                                            title = promptText.take(28),
                                            model = selectedModel
                                        )
                                        currentConversationId = newId
                                        newId
                                    }

                                    // Save User Message
                                    if (userId != "guest_user") {
                                        lonaRepository.addMessage(activeConvId, "user", promptText)
                                    } else {
                                        messages = messages + ChatMessage(sender = "user", content = promptText)
                                    }

                                    // Stream AI Response
                                    isStreaming = true
                                    streamingText = ""
                                    val accumulated = StringBuilder()

                                    aiRepository.streamResponse(promptText, selectedModel).collect { chunk ->
                                        accumulated.append(chunk)
                                        streamingText = accumulated.toString()
                                    }

                                    val finalAnswer = accumulated.toString().ifBlank { "Received response." }
                                    isStreaming = false
                                    streamingText = ""

                                    if (userId != "guest_user") {
                                        lonaRepository.addMessage(activeConvId, "assistant", finalAnswer)
                                    } else {
                                        messages = messages + ChatMessage(sender = "assistant", content = finalAnswer)
                                    }
                                }
                            },
                            onNewConversation = {
                                coroutineScope.launch {
                                    if (userId != "guest_user") {
                                        val newId = lonaRepository.createConversation("New Chat", selectedModel)
                                        currentConversationId = newId
                                    } else {
                                        messages = emptyList()
                                    }
                                    Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onNavigateToHistory = {
                                currentTab = MainNavTab.SETTINGS
                            },
                            onSpeakMessage = { text ->
                                voiceManager.speak(text)
                            },
                            onStartVoiceInput = toggleVoiceAction
                        )

                        MainNavTab.WORKSPACE -> CodingWorkspaceScreen(
                            currentProject = currentProject,
                            isGenerating = isWorkspaceGenerating,
                            generationStatus = workspaceGenerationStatus,
                            onGenerateCode = { prompt ->
                                coroutineScope.launch {
                                    isWorkspaceGenerating = true
                                    workspaceGenerationStatus = "Synthesizing HTML, CSS, & JS with $selectedModel..."

                                    val aiPrompt = "You are a master web developer. Generate a complete responsive website or component for: $prompt. " +
                                            "Provide ONLY 3 clearly labeled markdown code blocks: ```html, ```css, and ```javascript. " +
                                            "Do not write preamble."

                                    val result = aiRepository.generateResponse(aiPrompt, selectedModel)
                                    isWorkspaceGenerating = false

                                    if (result.isSuccess) {
                                        val text = result.getOrNull().orEmpty()
                                        parseGeneratedCode(text)?.let { (html, css, js) ->
                                            currentProject = currentProject.copy(
                                                htmlCode = html,
                                                cssCode = css,
                                                jsCode = js
                                            )
                                        }
                                        Toast.makeText(context, "Code generated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Generation notice: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onSaveProject = { project ->
                                currentProject = project
                                if (userId != "guest_user") {
                                    coroutineScope.launch {
                                        lonaRepository.saveProject(project)
                                    }
                                }
                            },
                            onSelectTemplate = { template ->
                                applyTemplate(template) { currentProject = it }
                            }
                        )

                        MainNavTab.TOOLS -> ToolsScreen(
                            onOpenWorkspaceTemplate = { template ->
                                applyTemplate(template) { currentProject = it }
                                currentTab = MainNavTab.WORKSPACE
                            },
                            onOpenChatPrompt = { prompt ->
                                chatPrefilledPrompt = prompt
                                currentTab = MainNavTab.CHAT
                            }
                        )

                        MainNavTab.SETTINGS -> SettingsScreen(
                            voiceManager = voiceManager,
                            aiRepository = aiRepository,
                            conversations = conversations,
                            edgeLightEnabled = edgeLightEnabled,
                            onToggleEdgeLight = { edgeLightEnabled = it },
                            onAccentColorSelected = { accentColor = it },
                            onDeleteConversation = { convId ->
                                coroutineScope.launch {
                                    lonaRepository.deleteConversation(convId)
                                    if (currentConversationId == convId) {
                                        currentConversationId = null
                                        messages = emptyList()
                                    }
                                }
                            },
                            onReopenConversation = { convId ->
                                currentConversationId = convId
                                currentTab = MainNavTab.CHAT
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LonaBottomNavigationBar(
    selectedTab: MainNavTab,
    onTabSelected: (MainNavTab) -> Unit,
    onCenterMicClicked: () -> Unit,
    isListening: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp)
            .background(CyberCard.copy(alpha = 0.96f))
            .border(1.dp, CyberCardBorder)
            .testTag("lona_bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 1. Home
            BottomNavTabItem(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = selectedTab == MainNavTab.HOME,
                onClick = { onTabSelected(MainNavTab.HOME) }
            )

            // 2. Chat
            BottomNavTabItem(
                label = "Chat",
                icon = Icons.Default.Chat,
                isSelected = selectedTab == MainNavTab.CHAT,
                onClick = { onTabSelected(MainNavTab.CHAT) }
            )

            // 3. Central Glowing Microphone Button Spacer
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .offset(y = (-8).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            if (isListening) listOf(ElectricCyan, NeonPink, ElectricCyan)
                            else listOf(NeonPurple, ElectricCyan, NeonPurple)
                        )
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCenterMicClicked
                    )
                    .testTag("bottom_bar_center_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // 4. Tools
            BottomNavTabItem(
                label = "Tools",
                icon = Icons.Default.Build,
                isSelected = selectedTab == MainNavTab.TOOLS,
                onClick = { onTabSelected(MainNavTab.TOOLS) }
            )

            // 5. Settings
            BottomNavTabItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                isSelected = selectedTab == MainNavTab.SETTINGS,
                onClick = { onTabSelected(MainNavTab.SETTINGS) }
            )
        }
    }
}

@Composable
fun BottomNavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("nav_tab_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ElectricCyan else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (isSelected) ElectricCyan else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

fun applyTemplate(template: String, onUpdate: (CodingProject) -> Unit) {
    when (template) {
        "login" -> onUpdate(
            CodingProject(
                name = "Cyberpunk Login",
                templateType = "login",
                htmlCode = defaultLoginHtml,
                cssCode = defaultLoginCss,
                jsCode = "console.log('Login initialized');"
            )
        )
        "registration" -> onUpdate(
            CodingProject(
                name = "Neon Registration",
                templateType = "registration",
                htmlCode = defaultRegHtml,
                cssCode = defaultRegCss,
                jsCode = "console.log('Registration initialized');"
            )
        )
        "landing" -> onUpdate(
            CodingProject(
                name = "Futuristic Landing Page",
                templateType = "landing",
                htmlCode = defaultLandingHtml,
                cssCode = defaultLandingCss,
                jsCode = "console.log('Landing Page initialized');"
            )
        )
        else -> onUpdate(
            CodingProject(
                name = "Custom Studio Project",
                templateType = "custom",
                htmlCode = "<h1>Hello from LONA AI Studio</h1><p>Edit HTML, CSS, and JS live!</p>",
                cssCode = "body { background: #07090e; color: #00f0ff; font-family: sans-serif; padding: 2rem; }",
                jsCode = "console.log('Custom project running');"
            )
        )
    }
}

fun parseGeneratedCode(text: String): Triple<String, String, String>? {
    try {
        var html = ""
        var css = ""
        var js = ""

        val htmlRegex = "```html\\s*([\\s\\S]*?)```".toRegex(RegexOption.IGNORE_CASE)
        val cssRegex = "```css\\s*([\\s\\S]*?)```".toRegex(RegexOption.IGNORE_CASE)
        val jsRegex = "```(?:javascript|js)\\s*([\\s\\S]*?)```".toRegex(RegexOption.IGNORE_CASE)

        htmlRegex.find(text)?.let { html = it.groupValues[1].trim() }
        cssRegex.find(text)?.let { css = it.groupValues[1].trim() }
        jsRegex.find(text)?.let { js = it.groupValues[1].trim() }

        if (html.isNotBlank() || css.isNotBlank() || js.isNotBlank()) {
            return Triple(html.ifBlank { "<h1>Generated Page</h1>" }, css, js)
        }
    } catch (e: Exception) {
        // Fallback
    }
    return null
}

// Built-in Templates
val defaultLoginHtml = """
<div class="login-card">
  <h2>LONA AI Auth</h2>
  <p>Secure Portal Login</p>
  <div class="input-group">
    <label>User Identifier</label>
    <input type="text" placeholder="neo_dev_01" />
  </div>
  <div class="input-group">
    <label>Access Key</label>
    <input type="password" placeholder="••••••••" />
  </div>
  <button onclick="alert('Access Granted!')">AUTHORIZE</button>
</div>
""".trimIndent()

val defaultLoginCss = """
body {
  margin: 0;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #07090e;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  color: #fff;
}
.login-card {
  background: #0f1422;
  border: 1.5px solid #a855f7;
  padding: 2.2rem;
  border-radius: 18px;
  width: 85%;
  max-width: 360px;
  box-shadow: 0 0 25px rgba(168, 85, 247, 0.3);
}
h2 { margin: 0 0 4px; color: #fff; }
p { margin: 0 0 1.5rem; color: #94a3b8; font-size: 0.9rem; }
.input-group { margin-bottom: 1.2rem; }
label { display: block; font-size: 0.8rem; color: #00f0ff; margin-bottom: 4px; }
input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.75rem;
  background: #141b2d;
  border: 1px solid #232d48;
  border-radius: 8px;
  color: #fff;
  outline: none;
}
input:focus { border-color: #00f0ff; }
button {
  width: 100%;
  padding: 0.85rem;
  background: linear-gradient(135deg, #a855f7, #00f0ff);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-weight: bold;
  cursor: pointer;
  letter-spacing: 1px;
}
""".trimIndent()

val defaultRegHtml = """
<div class="reg-card">
  <h2>New Operative</h2>
  <p>Initialize your quantum credentials</p>
  <input type="text" placeholder="Full Name" />
  <input type="email" placeholder="Neural Email" />
  <input type="password" placeholder="Key Phrase" />
  <button onclick="alert('Operative Registered!')">REGISTER</button>
</div>
""".trimIndent()

val defaultRegCss = """
body {
  margin: 0; min-height: 100vh; display: flex; justify-content: center; align-items: center;
  background: #07090e; font-family: sans-serif; color: #fff;
}
.reg-card {
  background: #0f1422; border: 1.5px solid #00f0ff; padding: 2rem; border-radius: 18px;
  width: 85%; max-width: 360px; box-shadow: 0 0 25px rgba(0, 240, 255, 0.3);
}
input {
  width: 100%; box-sizing: border-box; padding: 0.75rem; margin-bottom: 1rem;
  background: #141b2d; border: 1px solid #232d48; border-radius: 8px; color: #fff;
}
button {
  width: 100%; padding: 0.85rem; background: #00f0ff; border: none; border-radius: 8px;
  color: #000; font-weight: bold; cursor: pointer;
}
""".trimIndent()

val defaultLandingHtml = """
<div class="hero">
  <span class="badge">NEXT-GEN INTELLIGENCE</span>
  <h1>LONA AI SYSTEM</h1>
  <p>Experience ultra low latency conversational computing and autonomous code synthesis.</p>
  <div class="cta-row">
    <button class="primary" onclick="alert('Terminal Launched')">LAUNCH CONSOLE</button>
    <button class="secondary" onclick="alert('Documentation Opened')">READ DOCS</button>
  </div>
</div>
""".trimIndent()

val defaultLandingCss = """
body {
  margin: 0; min-height: 100vh; display: flex; justify-content: center; align-items: center;
  background: #07090e; font-family: sans-serif; color: #fff; text-align: center; padding: 1.5rem;
}
.hero { max-width: 500px; }
.badge {
  display: inline-block; padding: 4px 12px; background: rgba(168, 85, 247, 0.2);
  border: 1px solid #a855f7; border-radius: 20px; font-size: 0.75rem; color: #c084fc;
}
h1 { font-size: 2.2rem; margin: 1rem 0 0.5rem; letter-spacing: 2px; }
p { color: #94a3b8; font-size: 1rem; line-height: 1.6; margin-bottom: 2rem; }
.cta-row { display: flex; gap: 12px; justify-content: center; }
button.primary {
  padding: 0.85rem 1.5rem; background: linear-gradient(135deg, #a855f7, #00f0ff);
  border: none; border-radius: 10px; color: #fff; font-weight: bold; cursor: pointer;
}
button.secondary {
  padding: 0.85rem 1.5rem; background: transparent; border: 1px solid #232d48;
  border-radius: 10px; color: #94a3b8; font-weight: bold; cursor: pointer;
}
""".trimIndent()
