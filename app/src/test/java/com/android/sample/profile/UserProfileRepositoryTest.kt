package com.android.sample.profile

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var user: FirebaseUser
  private lateinit var usersCollection: CollectionReference
  private lateinit var userDocument: DocumentReference
  private lateinit var repository: UserProfileRepository

  @Before
  fun setUp() {
    auth = mock()
    firestore = mock()
    user = mock()
    usersCollection = mock()
    userDocument = mock()

    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("test-user-123")
    whenever(firestore.collection("users")).thenReturn(usersCollection)
    whenever(usersCollection.document("test-user-123")).thenReturn(userDocument)

    repository = UserProfileRepository(firestoreProvider = { firestore }, authProvider = { auth })
  }

  @Test
  fun saveProfile_successfully_saves_profile() = runTest {
    val profile =
        UserProfile(
            fullName = "John Doe",
            preferredName = "John",
            faculty = "IC",
            section = "IN",
            email = "john.doe@epfl.ch",
            phone = "+41 21 123 45 67",
            roleDescription = "Student")

    val task = Tasks.forResult<Void>(null)
    val payloadCaptor = argumentCaptor<Map<String, Any?>>()
    whenever(userDocument.set(payloadCaptor.capture(), any<SetOptions>())).thenReturn(task)

    repository.saveProfile(profile)

    verify(userDocument).set(any<Map<String, Any?>>(), any<SetOptions>())
    val payload = payloadCaptor.firstValue
    val profileMap = payload["profile"] as? Map<*, *>
    assertNotNull("Profile map should be present", profileMap)
    assertEquals("John Doe", profileMap?.get("fullName"))
    assertEquals("John", profileMap?.get("preferredName"))
    assertEquals("IC", profileMap?.get("faculty"))
    assertEquals("IN", profileMap?.get("section"))
    assertEquals("john.doe@epfl.ch", profileMap?.get("email"))
    assertEquals("+41 21 123 45 67", profileMap?.get("phone"))
    assertEquals("Student", profileMap?.get("roleDescription"))
    assertNotNull("profileUpdatedAt should be present", payload["profileUpdatedAt"])
  }

  @Test
  fun saveProfile_returns_early_when_auth_provider_throws() = runTest {
    val profile = UserProfile()
    val failingRepository =
        UserProfileRepository(
            firestoreProvider = { firestore },
            authProvider = { throw RuntimeException("Auth error") })

    // Should not throw, just return early
    failingRepository.saveProfile(profile)

    verify(userDocument, never()).set(any<Map<String, Any?>>(), any<SetOptions>())
  }

  @Test
  fun saveProfile_returns_early_when_no_current_user() = runTest {
    val profile = UserProfile()
    whenever(auth.currentUser).thenReturn(null)

    repository.saveProfile(profile)

    verify(userDocument, never()).set(any<Map<String, Any?>>(), any<SetOptions>())
  }

  @Test
  fun saveProfile_returns_early_when_firestore_provider_throws() = runTest {
    val profile = UserProfile()
    val failingRepository =
        UserProfileRepository(
            firestoreProvider = { throw RuntimeException("Firestore error") },
            authProvider = { auth })

    // Should not throw, just return early
    failingRepository.saveProfile(profile)

    // Verify that set was never called on the original userDocument
    verify(userDocument, never()).set(any<Map<String, Any?>>(), any<SetOptions>())
  }

  @Test
  fun loadProfile_successfully_loads_profile() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(true)
    val profileMap =
        mapOf(
            "fullName" to "Jane Smith",
            "preferredName" to "Jane",
            "faculty" to "SB",
            "section" to "CH",
            "email" to "jane.smith@epfl.ch",
            "phone" to "+41 21 987 65 43",
            "roleDescription" to "Professor")
    whenever(snapshot.get("profile")).thenReturn(profileMap)
    whenever(userDocument.get()).thenReturn(Tasks.forResult(snapshot))

    val result = repository.loadProfile()

    assertNotNull("Profile should be loaded", result)
    assertEquals("Jane Smith", result?.fullName)
    assertEquals("Jane", result?.preferredName)
    assertEquals("SB", result?.faculty)
    assertEquals("CH", result?.section)
    assertEquals("jane.smith@epfl.ch", result?.email)
    assertEquals("+41 21 987 65 43", result?.phone)
    assertEquals("Professor", result?.roleDescription)
  }

  @Test
  fun loadProfile_returns_null_when_auth_provider_throws() = runTest {
    val failingRepository =
        UserProfileRepository(
            firestoreProvider = { firestore },
            authProvider = { throw RuntimeException("Auth error") })

    val result = failingRepository.loadProfile()

    assertNull("Should return null when auth fails", result)
  }

  @Test
  fun loadProfile_returns_null_when_no_current_user() = runTest {
    whenever(auth.currentUser).thenReturn(null)

    val result = repository.loadProfile()

    assertNull("Should return null when no user", result)
  }

  @Test
  fun loadProfile_returns_null_when_firestore_provider_throws() = runTest {
    val failingRepository =
        UserProfileRepository(
            firestoreProvider = { throw RuntimeException("Firestore error") },
            authProvider = { auth })

    val result = failingRepository.loadProfile()

    assertNull("Should return null when firestore fails", result)
  }

  @Test
  fun loadProfile_returns_null_when_document_get_throws() = runTest {
    whenever(userDocument.get())
        .thenReturn(Tasks.forException<DocumentSnapshot>(RuntimeException("Get failed")))

    val result = repository.loadProfile()

    assertNull("Should return null when get fails", result)
  }

  @Test
  fun loadProfile_returns_null_when_document_does_not_exist() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(false)
    whenever(userDocument.get()).thenReturn(Tasks.forResult(snapshot))

    val result = repository.loadProfile()

    assertNull("Should return null when document doesn't exist", result)
  }

  @Test
  fun loadProfile_returns_null_when_profile_field_is_missing() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.get("profile")).thenReturn(null)
    whenever(userDocument.get()).thenReturn(Tasks.forResult(snapshot))

    val result = repository.loadProfile()

    assertNull("Should return null when profile field is missing", result)
  }

  @Test
  fun loadProfile_returns_null_when_profile_field_is_not_map() = runTest {
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.get("profile")).thenReturn("not a map")
    whenever(userDocument.get()).thenReturn(Tasks.forResult(snapshot))

    val result = repository.loadProfile()

    assertNull("Should return null when profile is not a map", result)
  }

  @Test
  fun toFirestoreMap_contains_all_fields() {
    val profile =
        UserProfile(
            fullName = "Test User",
            preferredName = "Test",
            faculty = "IC",
            section = "IN",
            email = "test@epfl.ch",
            phone = "+41 21 000 00 00",
            roleDescription = "Student")

    val map = profile.toFirestoreMap()

    assertEquals("Test User", map["fullName"])
    assertEquals("Test", map["preferredName"])
    assertEquals("IC", map["faculty"])
    assertEquals("IN", map["section"])
    assertEquals("test@epfl.ch", map["email"])
    assertEquals("+41 21 000 00 00", map["phone"])
    assertEquals("Student", map["roleDescription"])
    assertEquals(7, map.size)
  }

  @Test
  fun toFirestoreMap_handles_empty_strings() {
    val profile = UserProfile()

    val map = profile.toFirestoreMap()

    assertEquals("", map["fullName"])
    assertEquals("", map["preferredName"])
    assertEquals("", map["faculty"])
    assertEquals("", map["section"])
    assertEquals("", map["email"])
    assertEquals("", map["phone"])
    assertEquals("", map["roleDescription"])
  }

  @Test
  fun fromFirestore_creates_profile_with_all_fields() {
    val map =
        mapOf(
            "fullName" to "Alice Brown",
            "preferredName" to "Alice",
            "faculty" to "ENAC",
            "section" to "AR",
            "email" to "alice.brown@epfl.ch",
            "phone" to "+41 21 111 22 33",
            "roleDescription" to "Staff")

    val profile = UserProfile.fromFirestore(map)

    assertEquals("Alice Brown", profile.fullName)
    assertEquals("Alice", profile.preferredName)
    assertEquals("ENAC", profile.faculty)
    assertEquals("AR", profile.section)
    assertEquals("alice.brown@epfl.ch", profile.email)
    assertEquals("+41 21 111 22 33", profile.phone)
    assertEquals("Staff", profile.roleDescription)
  }

  @Test
  fun fromFirestore_uses_empty_string_when_field_missing() {
    val map = mapOf<String, Any?>()

    val profile = UserProfile.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun fromFirestore_uses_empty_string_when_field_is_null() {
    val map =
        mapOf(
            "fullName" to null,
            "preferredName" to null,
            "faculty" to null,
            "section" to null,
            "email" to null,
            "phone" to null,
            "roleDescription" to null)

    val profile = UserProfile.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun fromFirestore_uses_empty_string_when_field_is_not_string() {
    val map =
        mapOf(
            "fullName" to 123,
            "preferredName" to true,
            "faculty" to listOf("IC"),
            "section" to mapOf("key" to "value"),
            "email" to 456.789,
            "phone" to null,
            "roleDescription" to "Valid")

    val profile = UserProfile.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("", profile.email)
    assertEquals("", profile.phone)
    assertEquals("Valid", profile.roleDescription)
  }

  @Test
  fun fromFirestore_handles_partial_data() {
    val map = mapOf("fullName" to "Partial User", "email" to "partial@epfl.ch")

    val profile = UserProfile.fromFirestore(map)

    assertEquals("Partial User", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("partial@epfl.ch", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun roundtrip_toFirestoreMap_and_fromFirestore() {
    val original =
        UserProfile(
            fullName = "Roundtrip Test",
            preferredName = "Roundtrip",
            faculty = "IC",
            section = "IN",
            email = "roundtrip@epfl.ch",
            phone = "+41 21 999 88 77",
            roleDescription = "Test User")

    val map = original.toFirestoreMap()
    val restored = UserProfile.fromFirestore(map)

    assertEquals(original.fullName, restored.fullName)
    assertEquals(original.preferredName, restored.preferredName)
    assertEquals(original.faculty, restored.faculty)
    assertEquals(original.section, restored.section)
    assertEquals(original.email, restored.email)
    assertEquals(original.phone, restored.phone)
    assertEquals(original.roleDescription, restored.roleDescription)
  }
}
