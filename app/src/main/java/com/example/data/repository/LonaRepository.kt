package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.CodingProject
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LonaRepository(private val db: FirebaseFirestore? = null) {

    companion object {
        private fun resolveFirestore(context: Context): FirebaseFirestore? {
            return runCatching {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId) else FirebaseFirestore.getInstance()
            }.getOrNull()
        }
    }

    constructor(context: Context) : this(resolveFirestore(context))

    // Local in-memory caches to allow 100% functionality without requiring any login or cloud auth
    private val localUserProfile = MutableStateFlow(
        UserProfile(
            userId = "lona_user",
            displayName = "LONA Commander",
            email = "user@lona.ai",
            theme = "cyberpunk",
            accentColor = "#A855F7",
            selectedModel = "gemini-3.8-flash"
        )
    )
    private val localConversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val localMessages = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    private val localProjects = MutableStateFlow<List<CodingProject>>(emptyList())

    private fun getUserId(): String {
        return runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "lona_user"
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            return flow {
                val path = "users/$userId"
                emitAll(
                    database.collection("users").document(userId)
                        .snapshots()
                        .map { snapshot ->
                            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else localUserProfile.value
                        }
                        .catch { emit(localUserProfile.value) }
                )
            }
        }
        return localUserProfile.asStateFlow()
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        localUserProfile.value = profile
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            val uid = userAuth.uid
            val docRef = database.collection("users").document(uid)
            runCatching {
                val payload = hashMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to profile.displayName.ifBlank { "LONA Commander" },
                    "email" to profile.email.ifBlank { "user@lona.ai" },
                    "photoUrl" to profile.photoUrl,
                    "theme" to profile.theme,
                    "accentColor" to profile.accentColor,
                    "voiceName" to profile.voiceName,
                    "language" to profile.language,
                    "selectedModel" to profile.selectedModel,
                    "edgeLightEnabled" to profile.edgeLightEnabled,
                    "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        }
    }

    // --- Conversations ---

    fun observeConversations(): Flow<List<Conversation>> {
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            val uid = userAuth.uid
            return flow {
                emitAll(
                    database.collection("users").document(uid).collection("conversations")
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .snapshots()
                        .map { snapshot -> snapshot.toObjects(Conversation::class.java) }
                        .catch { emit(localConversations.value) }
                )
            }
        }
        return localConversations.asStateFlow()
    }

    suspend fun createConversation(title: String, model: String): String {
        val convId = UUID.randomUUID().toString()
        val uid = getUserId()
        val newConv = Conversation(
            id = convId,
            userId = uid,
            title = title.ifBlank { "New Conversation" },
            model = model,
            lastMessage = ""
        )
        localConversations.value = listOf(newConv) + localConversations.value

        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            runCatching {
                val colRef = database.collection("users").document(userAuth.uid).collection("conversations")
                val docRef = colRef.document(convId)
                val payload = hashMapOf<String, Any>(
                    "id" to convId,
                    "userId" to userAuth.uid,
                    "title" to title.ifBlank { "New Conversation" },
                    "model" to model,
                    "lastMessage" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        }
        return convId
    }

    suspend fun deleteConversation(conversationId: String) {
        localConversations.value = localConversations.value.filterNot { it.id == conversationId }
        localMessages.remove(conversationId)

        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            runCatching {
                database.collection("users").document(userAuth.uid).collection("conversations").document(conversationId).delete().await()
            }
        }
    }

    // --- Messages ---

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        val messageFlow = localMessages.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            val uid = userAuth.uid
            return flow {
                emitAll(
                    database.collection("users").document(uid).collection("conversations").document(conversationId).collection("messages")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .snapshots()
                        .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
                        .catch { emit(messageFlow.value) }
                )
            }
        }
        return messageFlow.asStateFlow()
    }

    suspend fun addMessage(conversationId: String, sender: String, content: String): String {
        val msgId = UUID.randomUUID().toString()
        val uid = getUserId()
        val newMsg = ChatMessage(
            id = msgId,
            userId = uid,
            conversationId = conversationId,
            sender = sender,
            content = content,
            status = "delivered"
        )
        val msgFlow = localMessages.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        msgFlow.value = msgFlow.value + newMsg

        // Update local conversation lastMessage
        localConversations.value = localConversations.value.map { conv ->
            if (conv.id == conversationId) conv.copy(lastMessage = content.take(120)) else conv
        }

        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            runCatching {
                val colRef = database.collection("users").document(userAuth.uid).collection("conversations").document(conversationId).collection("messages")
                val docRef = colRef.document(msgId)
                val payload = hashMapOf<String, Any>(
                    "id" to msgId,
                    "userId" to userAuth.uid,
                    "conversationId" to conversationId,
                    "sender" to sender,
                    "content" to content,
                    "status" to "delivered",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()

                val convRef = database.collection("users").document(userAuth.uid).collection("conversations").document(conversationId)
                convRef.update(
                    mapOf(
                        "lastMessage" to content.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        return msgId
    }

    // --- Coding Projects ---

    fun observeProjects(): Flow<List<CodingProject>> {
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            val uid = userAuth.uid
            return flow {
                emitAll(
                    database.collection("users").document(uid).collection("projects")
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .snapshots()
                        .map { snapshot -> snapshot.toObjects(CodingProject::class.java) }
                        .catch { emit(localProjects.value) }
                )
            }
        }
        return localProjects.asStateFlow()
    }

    suspend fun saveProject(project: CodingProject): String {
        val projectId = project.id.ifBlank { UUID.randomUUID().toString() }
        val updatedProject = project.copy(id = projectId)
        localProjects.value = listOf(updatedProject) + localProjects.value.filterNot { it.id == projectId }

        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            runCatching {
                val colRef = database.collection("users").document(userAuth.uid).collection("projects")
                val docRef = colRef.document(projectId)
                val payload = hashMapOf<String, Any>(
                    "id" to projectId,
                    "userId" to userAuth.uid,
                    "name" to project.name.ifBlank { "Untitled Project" },
                    "templateType" to project.templateType,
                    "htmlCode" to project.htmlCode,
                    "cssCode" to project.cssCode,
                    "jsCode" to project.jsCode,
                    "createdAt" to (project.createdAt ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        }
        return projectId
    }

    suspend fun deleteProject(projectId: String) {
        localProjects.value = localProjects.value.filterNot { it.id == projectId }
        val database = db
        val userAuth = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (database != null && userAuth != null) {
            runCatching {
                database.collection("users").document(userAuth.uid).collection("projects").document(projectId).delete().await()
            }
        }
    }
}
