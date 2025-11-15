package com.android.sample.logic

import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Pure JVM unit tests for UserProfileMapper. Tests mapping logic without Android dependencies. */
class UserProfileMapperTest {

  @Test
  fun `toFirestoreMap converts profile to map correctly`() {
    val profile =
        UserProfile(
            fullName = "John Doe",
            preferredName = "John",
            faculty = "IC",
            section = "CS",
            email = "john@epfl.ch",
            phone = "+41 21 693 0000",
            roleDescription = "Student")

    val map = UserProfileMapper.toFirestoreMap(profile)

    assertEquals("John Doe", map["fullName"])
    assertEquals("John", map["preferredName"])
    assertEquals("IC", map["faculty"])
    assertEquals("CS", map["section"])
    assertEquals("john@epfl.ch", map["email"])
    assertEquals("+41 21 693 0000", map["phone"])
    assertEquals("Student", map["roleDescription"])
    assertEquals(7, map.size)
  }

  @Test
  fun `toFirestoreMap handles empty profile`() {
    val profile = UserProfile()
    val map = UserProfileMapper.toFirestoreMap(profile)

    assertEquals("", map["fullName"])
    assertEquals("", map["preferredName"])
    assertEquals("", map["faculty"])
    assertEquals("", map["section"])
    assertEquals("", map["email"])
    assertEquals("", map["phone"])
    assertEquals("", map["roleDescription"])
  }

  @Test
  fun `fromFirestore converts map to profile correctly`() {
    val map =
        mapOf(
            "fullName" to "Jane Smith",
            "preferredName" to "Jane",
            "faculty" to "ENAC",
            "section" to "Civil",
            "email" to "jane@epfl.ch",
            "phone" to "+41 21 693 1111",
            "roleDescription" to "Teacher")

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("Jane Smith", profile.fullName)
    assertEquals("Jane", profile.preferredName)
    assertEquals("ENAC", profile.faculty)
    assertEquals("Civil", profile.section)
    assertEquals("jane@epfl.ch", profile.email)
    assertEquals("+41 21 693 1111", profile.phone)
    assertEquals("Teacher", profile.roleDescription)
  }

  @Test
  fun `fromFirestore handles missing fields with empty strings`() {
    val map = mapOf<String, Any?>()

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun `fromFirestore handles null values with empty strings`() {
    val map =
        mapOf(
            "fullName" to null,
            "preferredName" to null,
            "faculty" to null,
            "section" to null,
            "email" to null,
            "phone" to null,
            "roleDescription" to null)

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun `fromFirestore handles non-string values with empty strings`() {
    val map =
        mapOf(
            "fullName" to 123,
            "preferredName" to true,
            "faculty" to listOf("test"),
            "email" to mapOf("nested" to "value"))

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.email)
  }

  @Test
  fun `fromFirestore handles partial map`() {
    val map = mapOf("fullName" to "Test", "email" to "test@epfl.ch")

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("Test", profile.fullName)
    assertEquals("", profile.preferredName)
    assertEquals("", profile.faculty)
    assertEquals("", profile.section)
    assertEquals("test@epfl.ch", profile.email)
    assertEquals("", profile.phone)
    assertEquals("", profile.roleDescription)
  }

  @Test
  fun `round trip conversion preserves all fields`() {
    val original =
        UserProfile(
            fullName = "Round Trip",
            preferredName = "RT",
            faculty = "STI",
            section = "EE",
            email = "rt@epfl.ch",
            phone = "+41 21 693 2222",
            roleDescription = "PhD")

    val map = UserProfileMapper.toFirestoreMap(original)
    val reconstructed = UserProfileMapper.fromFirestore(map)

    assertEquals(original, reconstructed)
  }

  @Test
  fun `toFirestoreMap returns immutable map`() {
    val profile = UserProfile(fullName = "Test")
    val map = UserProfileMapper.toFirestoreMap(profile)

    assertNotNull(map)
    // Verify it's a proper map (not just checking size)
    assertEquals(7, map.keys.size)
  }

  @Test
  fun `fromFirestore handles empty strings correctly`() {
    val map =
        mapOf(
            "fullName" to "",
            "preferredName" to "",
            "faculty" to "",
            "section" to "",
            "email" to "",
            "phone" to "",
            "roleDescription" to "")

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("", profile.fullName)
    assertEquals("", profile.preferredName)
  }

  @Test
  fun `fromFirestore handles special characters in strings`() {
    val map =
        mapOf(
            "fullName" to "José María O'Connor-Smith",
            "preferredName" to "user_name-123",
            "email" to "test+tag@epfl.ch")

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals("José María O'Connor-Smith", profile.fullName)
    assertEquals("user_name-123", profile.preferredName)
    assertEquals("test+tag@epfl.ch", profile.email)
  }

  @Test
  fun `fromFirestore handles very long strings`() {
    val longString = "A".repeat(1000)
    val map = mapOf("fullName" to longString, "preferredName" to longString)

    val profile = UserProfileMapper.fromFirestore(map)

    assertEquals(longString, profile.fullName)
    assertEquals(longString, profile.preferredName)
  }
}
