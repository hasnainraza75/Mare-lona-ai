package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val theme: String = "Neon Purple",
    val accentColor: String = "#A855F7",
    val voiceName: String = "Korean Female (Lona)",
    val language: String = "Auto",
    val selectedModel: String = "gemini-3.8-flash",
    val edgeLightEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val model: String = "gemini-3.8-flash",
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val sender: String = "user", // "user" or "assistant"
    val content: String = "",
    val status: String = "sent",
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class CodingProject(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val templateType: String = "login",
    val htmlCode: String = "",
    val cssCode: String = "",
    val jsCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
