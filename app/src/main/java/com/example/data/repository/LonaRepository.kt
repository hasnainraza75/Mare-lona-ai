package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.CodingProject
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LonaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        require(profile.userId == uid) { "Profile userId must match authenticated user" }
        val docRef = db.collection("users").document(uid)
        try {
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "displayName" to profile.displayName.ifBlank { "Lona Explorer" },
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Conversations ---

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/conversations"
        emitAll(
            db.collection("users").document(uid).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Conversation::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createConversation(title: String, model: String): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("conversations")
        val docRef = colRef.document()
        try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "title" to title.ifBlank { "New Conversation" },
                "model" to model,
                "lastMessage" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Messages ---

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/conversations/$conversationId/messages"
        emitAll(
            db.collection("users").document(uid).collection("conversations").document(conversationId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addMessage(conversationId: String, sender: String, content: String): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("conversations").document(conversationId).collection("messages")
        val docRef = colRef.document()
        try {
            val payload = hashMapOf<String, Any>(
                "id" to docRef.id,
                "userId" to uid,
                "conversationId" to conversationId,
                "sender" to sender,
                "content" to content,
                "status" to "delivered",
                "timestamp" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Update conversation lastMessage
            val convRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
            convRef.update(
                mapOf(
                    "lastMessage" to content.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Coding Projects ---

    fun observeProjects(): Flow<List<CodingProject>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CodingProject::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveProject(project: CodingProject): String {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("projects")
        val docId = project.id.ifBlank { colRef.document().id }
        val docRef = colRef.document(docId)
        try {
            val payload = hashMapOf<String, Any>(
                "id" to docId,
                "userId" to uid,
                "name" to project.name.ifBlank { "Untitled Project" },
                "templateType" to project.templateType,
                "htmlCode" to project.htmlCode,
                "cssCode" to project.cssCode,
                "jsCode" to project.jsCode,
                "createdAt" to (project.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
