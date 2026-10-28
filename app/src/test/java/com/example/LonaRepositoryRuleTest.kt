package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.example.data.repository.LonaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LonaRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authenticatedUserCanSaveAndRetrieveProfile() = runBlocking {
    val uid = signInTestUser("lona_tester@example.com")
    val repo = LonaRepository(firestore)

    val profile = UserProfile(
      userId = uid,
      displayName = "Lona Tester",
      email = "lona_tester@example.com",
      theme = "Neon Purple",
      accentColor = "#A855F7",
      voiceName = "Korean Female (Lona)",
      language = "Auto"
    )

    repo.saveUserProfile(profile)

    val loaded = repo.observeUserProfile(uid).first()
    assertNotNull(loaded)
    assertEquals(uid, loaded?.userId)
    assertEquals("Lona Tester", loaded?.displayName)
  }

  @Test
  fun authenticatedUserCanCreateConversationAndAddMessage() = runBlocking {
    val uid = signInTestUser("conversation_user@example.com")
    val repo = LonaRepository(firestore)

    val convId = repo.createConversation("Test Chat", "gemini-3.8-flash")
    assertTrue(convId.isNotBlank())

    val msgId = repo.addMessage(convId, "user", "Hello Lona!")
    assertTrue(msgId.isNotBlank())

    val messages = repo.observeMessages(convId).first()
    assertTrue(messages.isNotEmpty())
    assertEquals("Hello Lona!", messages.first().content)
  }

  @Test
  fun unauthenticatedUserThrowsOnWrite() = runBlocking {
    auth.signOut()
    val repo = LonaRepository(firestore)

    try {
      repo.createConversation("Forbidden Chat", "gemini-3.8-flash")
      fail("Expected IllegalStateException for unauthenticated write")
    } catch (expected: IllegalStateException) {
      assertTrue(expected.message?.contains("User must be signed in") == true)
    }
  }
}
