package com.android.sample.signinscreen

import com.android.sample.authentification.AuthTags
import org.junit.Assert.*
import org.junit.Test

class AuthTagsTest {

  @Test
  fun AuthTags_has_correct_tag_constants() {
    assertEquals("auth_root", AuthTags.Root)
    assertEquals("auth_card", AuthTags.Card)
    assertEquals("auth_logos_row", AuthTags.LogosRow)
    assertEquals("auth_logo_epfl", AuthTags.LogoEpfl)
    assertEquals("auth_logo_point", AuthTags.LogoPoint)
    assertEquals("auth_logo_euler", AuthTags.LogoEuler)
    assertEquals("auth_title", AuthTags.Title)
    assertEquals("auth_subtitle", AuthTags.Subtitle)
    assertEquals("auth_or_separator", AuthTags.OrSeparator)
    assertEquals("auth_btn_ms", AuthTags.BtnMicrosoft)
    assertEquals("auth_btn_switch", AuthTags.BtnSwitchEdu)
    assertEquals("auth_ms_progress", AuthTags.MsProgress)
    assertEquals("auth_switch_progress", AuthTags.SwitchProgress)
    assertEquals("auth_terms_text", AuthTags.TermsText)
    assertEquals("auth_by_epfl_text", AuthTags.ByEpflText)
  }

  @Test
  fun AuthTags_constants_are_not_empty() {
    assertTrue(AuthTags.Root.isNotEmpty())
    assertTrue(AuthTags.Card.isNotEmpty())
    assertTrue(AuthTags.LogosRow.isNotEmpty())
    assertTrue(AuthTags.LogoEpfl.isNotEmpty())
    assertTrue(AuthTags.LogoPoint.isNotEmpty())
    assertTrue(AuthTags.LogoEuler.isNotEmpty())
    assertTrue(AuthTags.Title.isNotEmpty())
    assertTrue(AuthTags.Subtitle.isNotEmpty())
    assertTrue(AuthTags.OrSeparator.isNotEmpty())
    assertTrue(AuthTags.BtnMicrosoft.isNotEmpty())
    assertTrue(AuthTags.BtnSwitchEdu.isNotEmpty())
    assertTrue(AuthTags.MsProgress.isNotEmpty())
    assertTrue(AuthTags.SwitchProgress.isNotEmpty())
    assertTrue(AuthTags.TermsText.isNotEmpty())
    assertTrue(AuthTags.ByEpflText.isNotEmpty())
  }

  @Test
  fun AuthTags_constants_are_unique() {
    val tags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)

    assertEquals(tags.size, tags.distinct().size)
  }

  @Test
  fun AuthTags_object_is_not_null() {
    assertNotNull(AuthTags)
  }

  @Test
  fun AuthTags_constants_follow_naming_convention() {
    assertTrue(AuthTags.Root.startsWith("auth_"))
    assertTrue(AuthTags.Card.startsWith("auth_"))
    assertTrue(AuthTags.LogosRow.startsWith("auth_"))
    assertTrue(AuthTags.LogoEpfl.startsWith("auth_"))
    assertTrue(AuthTags.LogoPoint.startsWith("auth_"))
    assertTrue(AuthTags.LogoEuler.startsWith("auth_"))
    assertTrue(AuthTags.Title.startsWith("auth_"))
    assertTrue(AuthTags.Subtitle.startsWith("auth_"))
    assertTrue(AuthTags.OrSeparator.startsWith("auth_"))
    assertTrue(AuthTags.BtnMicrosoft.startsWith("auth_"))
    assertTrue(AuthTags.BtnSwitchEdu.startsWith("auth_"))
    assertTrue(AuthTags.MsProgress.startsWith("auth_"))
    assertTrue(AuthTags.SwitchProgress.startsWith("auth_"))
    assertTrue(AuthTags.TermsText.startsWith("auth_"))
    assertTrue(AuthTags.ByEpflText.startsWith("auth_"))
  }

  @Test
  fun AuthTags_does_not_contain_whitespace() {
    assertFalse(AuthTags.Root.contains(" "))
    assertFalse(AuthTags.Card.contains(" "))
    assertFalse(AuthTags.LogosRow.contains(" "))
    assertFalse(AuthTags.LogoEpfl.contains(" "))
    assertFalse(AuthTags.LogoPoint.contains(" "))
    assertFalse(AuthTags.LogoEuler.contains(" "))
    assertFalse(AuthTags.Title.contains(" "))
    assertFalse(AuthTags.Subtitle.contains(" "))
    assertFalse(AuthTags.OrSeparator.contains(" "))
    assertFalse(AuthTags.BtnMicrosoft.contains(" "))
    assertFalse(AuthTags.BtnSwitchEdu.contains(" "))
    assertFalse(AuthTags.MsProgress.contains(" "))
    assertFalse(AuthTags.SwitchProgress.contains(" "))
    assertFalse(AuthTags.TermsText.contains(" "))
    assertFalse(AuthTags.ByEpflText.contains(" "))
  }

  @Test
  fun AuthTags_constants_length_check() {
    assertTrue(AuthTags.Root.length > 5)
    assertTrue(AuthTags.Card.length > 5)
    assertTrue(AuthTags.LogosRow.length > 5)
    assertTrue(AuthTags.LogoEpfl.length > 5)
    assertTrue(AuthTags.LogoPoint.length > 5)
    assertTrue(AuthTags.LogoEuler.length > 5)
    assertTrue(AuthTags.Title.length > 5)
    assertTrue(AuthTags.Subtitle.length > 5)
    assertTrue(AuthTags.OrSeparator.length > 5)
    assertTrue(AuthTags.BtnMicrosoft.length > 5)
    assertTrue(AuthTags.BtnSwitchEdu.length > 5)
    assertTrue(AuthTags.MsProgress.length > 5)
    assertTrue(AuthTags.SwitchProgress.length > 5)
    assertTrue(AuthTags.TermsText.length > 5)
    assertTrue(AuthTags.ByEpflText.length > 5)
  }

  @Test
  fun AuthTags_is_singleton_object() {
    val instance1 = AuthTags
    val instance2 = AuthTags
    assertSame(instance1, instance2)
  }

  @Test
  fun AuthTags_object_class_name() {
    assertEquals("AuthTags", AuthTags::class.java.simpleName)
  }

  @Test
  fun AuthTags_root_tag_is_consistent() {
    val root = AuthTags.Root
    assertTrue(root.startsWith("auth_"))
    assertTrue(root.endsWith("root"))
  }

  @Test
  fun AuthTags_logo_tags_contain_logo() {
    assertTrue(AuthTags.LogoEpfl.contains("logo"))
    assertTrue(AuthTags.LogoEuler.contains("logo"))
    assertTrue(AuthTags.LogoPoint.contains("logo"))
  }

  @Test
  fun AuthTags_button_tags_contain_btn() {
    assertTrue(AuthTags.BtnMicrosoft.contains("btn"))
    assertTrue(AuthTags.BtnSwitchEdu.contains("btn"))
  }

  @Test
  fun AuthTags_progress_tags_contain_progress() {
    assertTrue(AuthTags.MsProgress.contains("progress"))
    assertTrue(AuthTags.SwitchProgress.contains("progress"))
  }

  @Test
  fun AuthTags_constants_have_expected_prefix() {
    val expectedPrefix = "auth_"
    assertTrue(AuthTags.Root.startsWith(expectedPrefix))
    assertTrue(AuthTags.Card.startsWith(expectedPrefix))
    assertTrue(AuthTags.LogosRow.startsWith(expectedPrefix))
    assertTrue(AuthTags.LogoEpfl.startsWith(expectedPrefix))
    assertTrue(AuthTags.LogoPoint.startsWith(expectedPrefix))
    assertTrue(AuthTags.LogoEuler.startsWith(expectedPrefix))
    assertTrue(AuthTags.Title.startsWith(expectedPrefix))
    assertTrue(AuthTags.Subtitle.startsWith(expectedPrefix))
    assertTrue(AuthTags.OrSeparator.startsWith(expectedPrefix))
    assertTrue(AuthTags.BtnMicrosoft.startsWith(expectedPrefix))
    assertTrue(AuthTags.BtnSwitchEdu.startsWith(expectedPrefix))
    assertTrue(AuthTags.MsProgress.startsWith(expectedPrefix))
    assertTrue(AuthTags.SwitchProgress.startsWith(expectedPrefix))
    assertTrue(AuthTags.TermsText.startsWith(expectedPrefix))
    assertTrue(AuthTags.ByEpflText.startsWith(expectedPrefix))
  }

  @Test
  fun AuthTags_action_buttons_have_btn_suffix() {
    assertTrue(AuthTags.BtnMicrosoft.contains("btn"))
    assertTrue(AuthTags.BtnSwitchEdu.contains("btn"))
  }

  @Test
  fun AuthTags_constants_are_immutable() {
    val root1 = AuthTags.Root
    val root2 = AuthTags.Root
    assertSame(root1, root2)
    assertEquals(root1, root2)
  }

  @Test
  fun AuthTags_tags_do_not_contain_special_chars() {
    val specialChars = listOf("@", "#", "$", "%", "&", "*", "(", ")", "+", "=")
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      specialChars.forEach { char ->
        assertFalse("Tag $tag should not contain $char", tag.contains(char))
      }
    }
  }

  @Test
  fun AuthTags_tags_use_underscore_separator() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      assertTrue("Tag $tag should use underscore separator", tag.contains("_"))
    }
  }

  @Test
  fun AuthTags_tags_are_lowercase() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag -> assertEquals("Tag $tag should be lowercase", tag.lowercase(), tag) }
  }

  @Test
  fun AuthTags_constants_have_reasonable_length() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      assertTrue("Tag $tag should be at least 8 chars", tag.length >= 8)
      assertTrue("Tag $tag should not exceed 30 chars", tag.length <= 30)
    }
  }

  @Test
  fun AuthTags_all_tags_are_accessible() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    assertEquals(15, allTags.size)
  }

  @Test
  fun AuthTags_strings_are_not_blank() {
    assertTrue(AuthTags.Root.isNotBlank())
    assertTrue(AuthTags.Card.isNotBlank())
    assertTrue(AuthTags.LogosRow.isNotBlank())
    assertTrue(AuthTags.LogoEpfl.isNotBlank())
    assertTrue(AuthTags.LogoPoint.isNotBlank())
    assertTrue(AuthTags.LogoEuler.isNotBlank())
    assertTrue(AuthTags.Title.isNotBlank())
    assertTrue(AuthTags.Subtitle.isNotBlank())
    assertTrue(AuthTags.OrSeparator.isNotBlank())
    assertTrue(AuthTags.BtnMicrosoft.isNotBlank())
    assertTrue(AuthTags.BtnSwitchEdu.isNotBlank())
    assertTrue(AuthTags.MsProgress.isNotBlank())
    assertTrue(AuthTags.SwitchProgress.isNotBlank())
    assertTrue(AuthTags.TermsText.isNotBlank())
    assertTrue(AuthTags.ByEpflText.isNotBlank())
  }

  @Test
  fun AuthTags_tags_do_not_start_with_underscore() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      assertFalse("Tag should not start with underscore: $tag", tag.startsWith("_"))
    }
  }

  @Test
  fun AuthTags_tags_do_not_end_with_underscore() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      assertFalse("Tag should not end with underscore: $tag", tag.endsWith("_"))
    }
  }

  @Test
  fun AuthTags_no_consecutive_underscores() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      assertFalse("Tag should not contain consecutive underscores: $tag", tag.contains("__"))
    }
  }

  @Test
  fun AuthTags_tags_have_minimum_word_count() {
    val allTags =
        listOf(
            AuthTags.Root,
            AuthTags.Card,
            AuthTags.LogosRow,
            AuthTags.LogoEpfl,
            AuthTags.LogoPoint,
            AuthTags.LogoEuler,
            AuthTags.Title,
            AuthTags.Subtitle,
            AuthTags.OrSeparator,
            AuthTags.BtnMicrosoft,
            AuthTags.BtnSwitchEdu,
            AuthTags.MsProgress,
            AuthTags.SwitchProgress,
            AuthTags.TermsText,
            AuthTags.ByEpflText)
    allTags.forEach { tag ->
      val wordCount = tag.split("_").size
      assertTrue("Tag should have at least 2 words: $tag", wordCount >= 2)
    }
  }

  @Test
  fun AuthTags_constants_can_be_used_in_equality() {
    assertEquals(AuthTags.Root, "auth_root")
    assertEquals(AuthTags.Card, "auth_card")
    assertNotEquals(AuthTags.Root, AuthTags.Card)
  }

  @Test
  fun AuthTags_tags_hashcode_consistency() {
    val root1 = AuthTags.Root.hashCode()
    val root2 = AuthTags.Root.hashCode()
    assertEquals(root1, root2)
  }

  @Test
  fun AuthTags_tags_toString_returns_value() {
    assertEquals("auth_root", AuthTags.Root.toString())
    assertEquals("auth_card", AuthTags.Card.toString())
  }

  @Test
  fun AuthTags_object_hashcode_consistency() {
    val hash1 = AuthTags.hashCode()
    val hash2 = AuthTags.hashCode()
    assertEquals(hash1, hash2)
  }

  @Test
  fun AuthTags_object_toString_contains_class_name() {
    val toString = AuthTags.toString()
    assertTrue(toString.contains("AuthTags"))
  }

  @Test
  fun AuthTags_all_button_tags_share_btn_pattern() {
    val buttonTags = listOf(AuthTags.BtnMicrosoft, AuthTags.BtnSwitchEdu)
    buttonTags.forEach { tag ->
      assertTrue("Button tag should contain 'btn': $tag", tag.contains("btn"))
    }
  }

  @Test
  fun AuthTags_text_tags_contain_text() {
    assertTrue(AuthTags.TermsText.contains("text"))
    assertTrue(AuthTags.ByEpflText.contains("text"))
  }

  @Test
  fun AuthTags_logo_tags_are_distinct() {
    assertNotEquals(AuthTags.LogoEpfl, AuthTags.LogoEuler)
    assertNotEquals(AuthTags.LogoEpfl, AuthTags.LogoPoint)
    assertNotEquals(AuthTags.LogoEuler, AuthTags.LogoPoint)
  }

  @Test
  fun AuthTags_button_tags_are_distinct() {
    assertNotEquals(AuthTags.BtnMicrosoft, AuthTags.BtnSwitchEdu)
  }

  @Test
  fun AuthTags_progress_tags_are_distinct() {
    assertNotEquals(AuthTags.MsProgress, AuthTags.SwitchProgress)
  }
}
