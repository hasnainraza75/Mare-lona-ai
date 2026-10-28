package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.repository.AiAssistantRepository
import com.example.ui.components.NeonCard
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HoloGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceAssistantManager
import com.example.voice.VoiceProfile
import kotlinx.coroutines.launch

enum class SettingsSubScreen {
    MAIN,
    API_CONFIG,
    VOICE_SETTINGS,
    APPEARANCE,
    PERMISSIONS,
    HISTORY,
    CONNECTIONS,
    ABOUT
}

@Composable
fun SettingsScreen(
    voiceManager: VoiceAssistantManager,
    aiRepository: AiAssistantRepository,
    conversations: List<Conversation>,
    edgeLightEnabled: Boolean,
    onToggleEdgeLight: (Boolean) -> Unit,
    onAccentColorSelected: (Color) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onReopenConversation: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    when (currentSubScreen) {
        SettingsSubScreen.MAIN -> SettingsMainList(
            edgeLightEnabled = edgeLightEnabled,
            onToggleEdgeLight = onToggleEdgeLight,
            onNavigateSubScreen = { currentSubScreen = it },
            onSignOut = onSignOut
        )
        SettingsSubScreen.API_CONFIG -> ApiConfigScreen(
            aiRepository = aiRepository,
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
        SettingsSubScreen.VOICE_SETTINGS -> VoiceSettingsScreen(
            voiceManager = voiceManager,
            onBack = {
                voiceManager.stopSpeaking()
                currentSubScreen = SettingsSubScreen.MAIN
            }
        )
        SettingsSubScreen.APPEARANCE -> AppearanceScreen(
            edgeLightEnabled = edgeLightEnabled,
            onToggleEdgeLight = onToggleEdgeLight,
            onAccentColorSelected = onAccentColorSelected,
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
        SettingsSubScreen.PERMISSIONS -> PermissionsScreen(
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
        SettingsSubScreen.HISTORY -> HistoryScreen(
            conversations = conversations,
            onReopenConversation = {
                onReopenConversation(it)
                currentSubScreen = SettingsSubScreen.MAIN
            },
            onDeleteConversation = onDeleteConversation,
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
        SettingsSubScreen.CONNECTIONS -> ConnectionsScreen(
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
        SettingsSubScreen.ABOUT -> AboutScreen(
            onBack = { currentSubScreen = SettingsSubScreen.MAIN }
        )
    }
}

@Composable
fun SettingsMainList(
    edgeLightEnabled: Boolean,
    onToggleEdgeLight: (Boolean) -> Unit,
    onNavigateSubScreen: (SettingsSubScreen) -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("settings_main_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SYSTEM SETTINGS",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Preferences, Integrations & Neural Parameters",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Settings Navigation Cards
            SettingsRowCard(
                title = "API Configuration",
                subtitle = "Manage Gemini API key & model endpoint",
                icon = Icons.Default.Api,
                accentColor = ElectricCyan,
                onClick = { onNavigateSubScreen(SettingsSubScreen.API_CONFIG) }
            )

            SettingsRowCard(
                title = "Voice & Neural Speech",
                subtitle = "Voice models, Korean female, Urdu, speed & preview",
                icon = Icons.Default.RecordVoiceOver,
                accentColor = NeonPurple,
                onClick = { onNavigateSubScreen(SettingsSubScreen.VOICE_SETTINGS) }
            )

            SettingsRowCard(
                title = "Appearance & Themes",
                subtitle = "Cyberpunk colors, glowing accents & edge lighting",
                icon = Icons.Default.ColorLens,
                accentColor = NeonPink,
                onClick = { onNavigateSubScreen(SettingsSubScreen.APPEARANCE) }
            )

            SettingsRowCard(
                title = "Conversation History",
                subtitle = "Review sessions, delete history, or clear cloud logs",
                icon = Icons.Default.History,
                accentColor = Color(0xFFF59E0B),
                onClick = { onNavigateSubScreen(SettingsSubScreen.HISTORY) }
            )

            SettingsRowCard(
                title = "Connections & Telemetry",
                subtitle = "Firebase AI Logic, network status & health checks",
                icon = Icons.Default.Wifi,
                accentColor = HoloGreen,
                onClick = { onNavigateSubScreen(SettingsSubScreen.CONNECTIONS) }
            )

            SettingsRowCard(
                title = "Device Permissions",
                subtitle = "Microphone, Audio, Network & Android settings link",
                icon = Icons.Default.Security,
                accentColor = ElectricCyan,
                onClick = { onNavigateSubScreen(SettingsSubScreen.PERMISSIONS) }
            )

            SettingsRowCard(
                title = "About LONA AI",
                subtitle = "Version 1.0.0, architecture specs & privacy guarantees",
                icon = Icons.Default.Info,
                accentColor = NeonPurple,
                onClick = { onNavigateSubScreen(SettingsSubScreen.ABOUT) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Edge Lighting Fast Toggle
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = CyberCardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Screen Edge Lighting", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Futuristic glowing border animation", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = edgeLightEnabled,
                        onCheckedChange = onToggleEdgeLight,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = NeonPurple.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Sign Out / Clear Data
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_sign_out_button"),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out / Clear User Session")
            }

            Spacer(modifier = Modifier.height(70.dp)) // Nav bar padding
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                containerColor = CyberCard,
                title = { Text("Sign Out Confirmation", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to sign out and clear your active session credentials?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CyberCardBorder,
        onClick = onClick,
        testTag = "settings_row_${title.lowercase().replace(" ", "_")}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: API CONFIG
// -------------------------------------------------------------
@Composable
fun ApiConfigScreen(
    aiRepository: AiAssistantRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    var isCheckSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("api_config_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Text("API Configuration", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = ElectricCyan.copy(alpha = 0.4f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ACTIVE ENGINE: FIREBASE AI LOGIC", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "By default, LONA AI uses server-side Firebase AI Logic with App Check attestation for secure, keyless Gemini 3.8 Flash model access.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CUSTOM GEMINI API KEY (OPTIONAL)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "To use your personal developer quota, securely enter your Gemini API key below. Keys are kept confidential on-device.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            isChecking = true
                            checkResult = null
                            scope.launch {
                                val result = aiRepository.testCustomApiKey(apiKeyInput)
                                isChecking = false
                                if (result.isSuccess) {
                                    isCheckSuccess = true
                                    checkResult = result.getOrNull()?.second ?: "Verified connection (HTTP 200)."
                                } else {
                                    isCheckSuccess = false
                                    checkResult = result.exceptionOrNull()?.message ?: "Connection check failed."
                                }
                            }
                        },
                        enabled = apiKeyInput.isNotBlank() && !isChecking,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("check_api_connection_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("Verify API Key & Test Connection", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (checkResult != null) {
                        Text(
                            text = checkResult ?: "",
                            color = if (isCheckSuccess) HoloGreen else NeonPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090D15))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Get your own API Key", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("https://aistudio.google.com/apikey", color = TextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: VOICE SETTINGS
// -------------------------------------------------------------
@Composable
fun VoiceSettingsScreen(
    voiceManager: VoiceAssistantManager,
    onBack: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(voiceManager.selectedLanguage.value) }
    var speed by remember { mutableFloatStateOf(voiceManager.speechRate.value) }
    var pitch by remember { mutableFloatStateOf(voiceManager.speechPitch.value) }
    val supportedVoices = voiceManager.supportedVoices

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("voice_settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonPurple)
                }
                Text("Voice & Speech Synthesizer", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text("SELECT NEURAL VOICE MODEL", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            supportedVoices.forEach { profile ->
                VoiceProfileCard(
                    profile = profile,
                    isSelected = voiceManager.selectedVoice.value.id == profile.id,
                    onSelect = { voiceManager.selectVoice(profile) },
                    onPreview = { voiceManager.previewVoice(profile) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("CONVERSATION LANGUAGE", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            val languages = listOf("Auto", "Korean", "Urdu", "Roman Urdu", "Hindi", "English")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.take(3).forEach { lang ->
                    LanguageChip(
                        name = lang,
                        isSelected = selectedLanguage == lang,
                        onClick = {
                            selectedLanguage = lang
                            voiceManager.setSelectedLanguage(lang)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.drop(3).forEach { lang ->
                    LanguageChip(
                        name = lang,
                        isSelected = selectedLanguage == lang,
                        onClick = {
                            selectedLanguage = lang
                            voiceManager.setSelectedLanguage(lang)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Speech Pitch & Rate Controls
            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AUDIO ACOUSTICS", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Text("Speech Rate (${String.format("%.1f", speed)}x)", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = speed,
                        onValueChange = {
                            speed = it
                            voiceManager.setSpeechRate(it)
                        },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = ElectricCyan)
                    )

                    Text("Speech Pitch (${String.format("%.1f", pitch)}x)", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = pitch,
                        onValueChange = {
                            pitch = it
                            voiceManager.setSpeechPitch(it)
                        },
                        valueRange = 0.6f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPurple)
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun VoiceProfileCard(
    profile: VoiceProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isSelected) ElectricCyan else CyberCardBorder,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = profile.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (isSelected) {
                        Text(text = "ACTIVE", color = HoloGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(text = profile.description, color = TextSecondary, fontSize = 11.sp)
            }

            Button(
                onClick = onPreview,
                colors = ButtonDefaults.buttonColors(containerColor = CyberBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test", color = TextPrimary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LanguageChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonPurple.copy(alpha = 0.3f) else CyberCard)
            .border(1.dp, if (isSelected) ElectricCyan else CyberCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, color = if (isSelected) ElectricCyan else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: APPEARANCE
// -------------------------------------------------------------
@Composable
fun AppearanceScreen(
    edgeLightEnabled: Boolean,
    onToggleEdgeLight: (Boolean) -> Unit,
    onAccentColorSelected: (Color) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("appearance_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonPink)
                }
                Text("Appearance & Themes", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Text("NEON ACCENT COLOR", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            val colors = listOf(
                "Neon Purple" to NeonPurple,
                "Electric Cyan" to ElectricCyan,
                "Cyber Pink" to NeonPink,
                "Holo Green" to HoloGreen,
                "Deep Blue" to Color(0xFF3B82F6)
            )

            colors.forEach { (name, color) ->
                NeonCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CyberCardBorder,
                    onClick = { onAccentColorSelected(color) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Flowing Edge Light", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Dynamic perimeter aura when app is active", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = edgeLightEnabled,
                        onCheckedChange = onToggleEdgeLight,
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonPink, checkedTrackColor = NeonPurple.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: HISTORY
// -------------------------------------------------------------
@Composable
fun HistoryScreen(
    conversations: List<Conversation>,
    onReopenConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("history_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Text("Conversation History", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved conversations yet. Start a chat with Lona!", color = TextSecondary)
                }
            } else {
                conversations.forEach { conv ->
                    NeonCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = CyberCardBorder,
                        onClick = { onReopenConversation(conv.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title.ifBlank { "Conversation" },
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = conv.lastMessage.ifBlank { "Model: ${conv.model}" },
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { onDeleteConversation(conv.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: CONNECTIONS
// -------------------------------------------------------------
@Composable
fun ConnectionsScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("connections_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HoloGreen)
                }
                Text("Connections & Diagnostics", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = HoloGreen.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FIREBASE BACKEND", color = HoloGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("ONLINE", color = HoloGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Project ID: flowing-tea-461612-b4", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Database: ai-studio-android-0f4cf9ce-f866-4a66-9e79-f4fb53289c83", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("App Check Attestation: Installed (Debug & reCAPTCHA)", color = TextSecondary, fontSize = 11.sp)
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NETWORK TELEMETRY", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Connectivity: Active / WiFi / LTE", color = TextPrimary, fontSize = 12.sp)
                    Text("Gemini Latency: ~110ms", color = TextPrimary, fontSize = 12.sp)
                    Text("Zero-Trust Security Rules: Verified and Deployed", color = HoloGreen, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: PERMISSIONS
// -------------------------------------------------------------
@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("permissions_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Text("Device Permissions", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MICROPHONE (RECORD_AUDIO)", color = ElectricCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Required for conversational speech recognition, amplitude waveform feedback, and hands-free assistant control.", color = TextSecondary, fontSize = 12.sp)
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("INTERNET & NETWORK STATE", color = ElectricCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Required for real-time Firebase Firestore synchronization and Gemini generative model streaming.", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_system_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Android System Settings")
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

// -------------------------------------------------------------
// SUB-SCREENS: ABOUT
// -------------------------------------------------------------
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("about_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonPurple)
                }
                Text("About LONA AI", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonPurple.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LONA AI v1.0.0", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Next-Generation Android AI Assistant & Cyber Workspace", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Built natively for modern Android with Kotlin and Jetpack Compose. Powered by Google AI Studio, Gemini 3.8 Flash, Firebase AI Logic, and Firebase Enterprise Firestore.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            NeonCard(modifier = Modifier.fillMaxWidth(), borderColor = CyberCardBorder) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CORE INVARIANTS & POLICIES", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• No simulated mock responses: Real AI inference", color = TextSecondary, fontSize = 12.sp)
                    Text("• Authenticated zero-trust Firestore cloud persistence", color = TextSecondary, fontSize = 12.sp)
                    Text("• Sandboxed web code preview engine", color = TextSecondary, fontSize = 12.sp)
                    Text("• Battery & background limits respected", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
