package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.CodingProject
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
fun CodingWorkspaceScreen(
    currentProject: CodingProject,
    isGenerating: Boolean,
    generationStatus: String,
    onGenerateCode: (prompt: String) -> Unit,
    onSaveProject: (CodingProject) -> Unit,
    onSelectTemplate: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFileTab by remember { mutableIntStateOf(0) } // 0: index.html, 1: styles.css, 2: script.js, 3: Live Preview

    var htmlCode by remember(currentProject.htmlCode) { mutableStateOf(currentProject.htmlCode) }
    var cssCode by remember(currentProject.cssCode) { mutableStateOf(currentProject.cssCode) }
    var jsCode by remember(currentProject.jsCode) { mutableStateOf(currentProject.jsCode) }
    var promptInput by remember { mutableStateOf("") }
    var previewReloadKey by remember { mutableIntStateOf(0) }

    val fileTabs = listOf("index.html", "styles.css", "script.js", "Live Preview")

    // Combined HTML document for the sandboxed WebView preview
    val combinedHtml = remember(htmlCode, cssCode, jsCode) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            $cssCode
          </style>
        </head>
        <body>
          $htmlCode
          <script>
            $jsCode
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .testTag("coding_workspace_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Workspace Header: Title, Templates, Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = currentProject.name.ifBlank { "Neural Web Studio" },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sandboxed Multi-file Environment",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save Button
                    IconButton(
                        onClick = {
                            val updated = currentProject.copy(
                                htmlCode = htmlCode,
                                cssCode = cssCode,
                                jsCode = jsCode
                            )
                            onSaveProject(updated)
                            Toast.makeText(context, "Project saved to cloud", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberBackground)
                            .border(1.dp, CyberCardBorder, CircleShape)
                            .testTag("workspace_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save Project", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    }

                    // Copy active file
                    IconButton(
                        onClick = {
                            val textToCopy = when (selectedFileTab) {
                                0 -> htmlCode
                                1 -> cssCode
                                2 -> jsCode
                                else -> combinedHtml
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", textToCopy))
                            Toast.makeText(context, "Copied active file", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberBackground)
                            .border(1.dp, CyberCardBorder, CircleShape)
                            .testTag("workspace_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Quick Template Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TemplatePill("Login Form") { onSelectTemplate("login") }
                TemplatePill("Registration") { onSelectTemplate("registration") }
                TemplatePill("Landing Page") { onSelectTemplate("landing") }
                TemplatePill("Cyber Portfolio") { onSelectTemplate("portfolio") }
            }

            // File Tabs
            TabRow(
                selectedTabIndex = selectedFileTab,
                containerColor = CyberBackground,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedFileTab]),
                        color = if (selectedFileTab == 3) HoloGreen else ElectricCyan
                    )
                }
            ) {
                fileTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedFileTab == index,
                        onClick = { selectedFileTab = index },
                        text = {
                            Text(
                                text = tabTitle,
                                fontSize = 12.sp,
                                fontWeight = if (selectedFileTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFileTab == index) {
                                    if (index == 3) HoloGreen else ElectricCyan
                                } else TextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content: Code Editor or Sandboxed WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedFileTab) {
                    0 -> CodeEditorPane(code = htmlCode, onCodeChange = { htmlCode = it }, language = "HTML")
                    1 -> CodeEditorPane(code = cssCode, onCodeChange = { cssCode = it }, language = "CSS")
                    2 -> CodeEditorPane(code = jsCode, onCodeChange = { jsCode = it }, language = "JavaScript")
                    3 -> SandboxedPreviewPane(
                        htmlContent = combinedHtml,
                        reloadKey = previewReloadKey,
                        onReload = { previewReloadKey++ }
                    )
                }
            }

            // Bottom Prompt & AI Code Generation Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberCard)
                    .border(1.dp, CyberCardBorder)
                    .padding(12.dp)
            ) {
                if (isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ElectricCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = generationStatus.ifBlank { "Synthesizing code with Gemini..." },
                            color = ElectricCyan,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Describe website or page to build...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("workspace_prompt_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberBackground,
                            unfocusedContainerColor = CyberBackground
                        )
                    )

                    Button(
                        onClick = {
                            val prompt = promptInput.trim()
                            if (prompt.isNotBlank() && !isGenerating) {
                                promptInput = ""
                                onGenerateCode(prompt)
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("workspace_generate_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // Padding for bottom nav bar
        }
    }
}

@Composable
fun CodeEditorPane(
    code: String,
    onCodeChange: (String) -> Unit,
    language: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D15))
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxSize()
                .testTag("code_editor_field_${language.lowercase()}"),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0),
                lineHeight = 18.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SandboxedPreviewPane(
    htmlContent: String,
    reloadKey: Int,
    onReload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("sandboxed_preview_pane")
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                        }
                    }
                    loadDataWithBaseURL("https://lona.local/", htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                // Reload on changes
                webView.loadDataWithBaseURL("https://lona.local/", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Reload Button
        IconButton(
            onClick = onReload,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(CyberCard.copy(alpha = 0.9f))
                .border(1.5.dp, ElectricCyan, CircleShape)
                .testTag("preview_reload_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reload Preview", tint = ElectricCyan)
        }
    }
}

@Composable
fun TemplatePill(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberBackground)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = title, color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
