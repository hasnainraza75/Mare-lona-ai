package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ToolsScreen(
    onOpenWorkspaceTemplate: (template: String) -> Unit,
    onOpenChatPrompt: (prompt: String) -> Unit
) {
    val context = LocalContext.current
    var showApkDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("tools_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "NEURAL TOOLS SUITE",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "AI-Driven Development & Analysis Utilities",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    ToolItemCard(
                        title = "Website Generator",
                        desc = "Generate responsive modern landing pages with HTML/CSS",
                        icon = Icons.Default.Web,
                        accentColor = ElectricCyan,
                        onClick = { onOpenWorkspaceTemplate("landing") }
                    )
                }

                item {
                    ToolItemCard(
                        title = "Login / Auth Pages",
                        desc = "Craft responsive login & registration forms with validation",
                        icon = Icons.Default.Lock,
                        accentColor = NeonPurple,
                        onClick = { onOpenWorkspaceTemplate("login") }
                    )
                }

                item {
                    ToolItemCard(
                        title = "Web Code Studio",
                        desc = "Multi-file HTML, CSS, JavaScript live sandboxed editor",
                        icon = Icons.Default.Code,
                        accentColor = NeonPink,
                        onClick = { onOpenWorkspaceTemplate("custom") }
                    )
                }

                item {
                    ToolItemCard(
                        title = "Code Explainer & Fixer",
                        desc = "Diagnose syntax bugs, optimize algorithms & explain code",
                        icon = Icons.Default.BugReport,
                        accentColor = HoloGreen,
                        onClick = { onOpenChatPrompt("Please analyze, explain, and debug this code architecture for me:") }
                    )
                }

                item {
                    ToolItemCard(
                        title = "Live Web Preview",
                        desc = "Test rendering and responsiveness in an isolated sandboxed viewport",
                        icon = Icons.Default.Preview,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { onOpenWorkspaceTemplate("portfolio") }
                    )
                }

                item {
                    ToolItemCard(
                        title = "APK Build Workflow",
                        desc = "Official release compilation instructions and Gradle CI pipeline",
                        icon = Icons.Default.Android,
                        accentColor = ElectricCyan,
                        onClick = { showApkDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp)) // Nav bar clearance
        }

        // APK Build Instructions Dialog
        if (showApkDialog) {
            AlertDialog(
                onDismissRequest = { showApkDialog = false },
                containerColor = CyberCard,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = ElectricCyan)
                        Text("APK & Release Build Workflow", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "To build a signed Release APK / Android App Bundle (.aab):",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF090D15))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "gradle assembleRelease\n# Or for debug APK:\ngradle assembleDebug",
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Generated outputs are placed in app/build/outputs/apk/release/app-release.apk. AI Studio settings menu also provides one-click Export as ZIP, Push to GitHub, and direct APK generation.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Gradle Build Command", "gradle assembleRelease"))
                            Toast.makeText(context, "Copied build command", Toast.LENGTH_SHORT).show()
                            showApkDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("Copy Command")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showApkDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun ToolItemCard(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = accentColor.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        testTag = "tool_card_${title.lowercase().replace(" ", "_")}"
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 3
            )
        }
    }
}
