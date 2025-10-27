package com.android.sample.home

import org.junit.Assert.*
import org.junit.Test

class HomeScreenTest {

  @Test
  fun HomeTags_has_correct_tag_constants() {
    assertEquals("home_root", HomeTags.Root)
    assertEquals("home_menu_btn", HomeTags.MenuBtn)
    assertEquals("home_topright_btn", HomeTags.TopRightBtn)
    assertEquals("home_action1_btn", HomeTags.Action1Btn)
    assertEquals("home_action2_btn", HomeTags.Action2Btn)
    assertEquals("home_message_field", HomeTags.MessageField)
    assertEquals("home_send_btn", HomeTags.SendBtn)
    assertEquals("home_drawer", HomeTags.Drawer)
    assertEquals("home_topright_menu", HomeTags.TopRightMenu)
  }

  @Test
  fun HomeTags_constants_are_not_empty() {
    assertTrue(HomeTags.Root.isNotEmpty())
    assertTrue(HomeTags.MenuBtn.isNotEmpty())
    assertTrue(HomeTags.TopRightBtn.isNotEmpty())
    assertTrue(HomeTags.Action1Btn.isNotEmpty())
    assertTrue(HomeTags.Action2Btn.isNotEmpty())
    assertTrue(HomeTags.MessageField.isNotEmpty())
    assertTrue(HomeTags.SendBtn.isNotEmpty())
    assertTrue(HomeTags.Drawer.isNotEmpty())
    assertTrue(HomeTags.TopRightMenu.isNotEmpty())
  }

  @Test
  fun HomeTags_constants_are_unique() {
    val tags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)

    assertEquals(tags.size, tags.distinct().size)
  }

  @Test
  fun HomeTags_object_is_not_null() {
    assertNotNull(HomeTags)
  }

  @Test
  fun HomeTags_constants_follow_naming_convention() {
    assertTrue(HomeTags.Root.startsWith("home_"))
    assertTrue(HomeTags.MenuBtn.startsWith("home_"))
    assertTrue(HomeTags.TopRightBtn.startsWith("home_"))
    assertTrue(HomeTags.Action1Btn.startsWith("home_"))
    assertTrue(HomeTags.Action2Btn.startsWith("home_"))
    assertTrue(HomeTags.MessageField.startsWith("home_"))
    assertTrue(HomeTags.SendBtn.startsWith("home_"))
    assertTrue(HomeTags.Drawer.startsWith("home_"))
    assertTrue(HomeTags.TopRightMenu.startsWith("home_"))
  }

  @Test
  fun HomeTags_does_not_contain_whitespace() {
    assertFalse(HomeTags.Root.contains(" "))
    assertFalse(HomeTags.MenuBtn.contains(" "))
    assertFalse(HomeTags.TopRightBtn.contains(" "))
    assertFalse(HomeTags.Action1Btn.contains(" "))
    assertFalse(HomeTags.Action2Btn.contains(" "))
    assertFalse(HomeTags.MessageField.contains(" "))
    assertFalse(HomeTags.SendBtn.contains(" "))
    assertFalse(HomeTags.Drawer.contains(" "))
    assertFalse(HomeTags.TopRightMenu.contains(" "))
  }

  @Test
  fun HomeTags_constants_length_check() {
    assertTrue(HomeTags.Root.length > 5)
    assertTrue(HomeTags.MenuBtn.length > 5)
    assertTrue(HomeTags.TopRightBtn.length > 5)
    assertTrue(HomeTags.Action1Btn.length > 5)
    assertTrue(HomeTags.Action2Btn.length > 5)
    assertTrue(HomeTags.MessageField.length > 5)
    assertTrue(HomeTags.SendBtn.length > 5)
    assertTrue(HomeTags.Drawer.length > 5)
    assertTrue(HomeTags.TopRightMenu.length > 5)
  }

  @Test
  fun HomeTags_is_singleton_object() {
    val instance1 = HomeTags
    val instance2 = HomeTags
    assertSame(instance1, instance2)
  }

  @Test
  fun HomeTags_object_class_name() {
    assertEquals("HomeTags", HomeTags::class.java.simpleName)
  }

  @Test
  fun HomeTags_root_tag_is_consistent() {
    val root = HomeTags.Root
    assertTrue(root.startsWith("home_"))
    assertTrue(root.endsWith("root"))
  }

  @Test
  fun HomeTags_menu_btn_tag_is_consistent() {
    val menuBtn = HomeTags.MenuBtn
    assertTrue(menuBtn.contains("menu"))
    assertTrue(menuBtn.contains("btn"))
  }

  @Test
  fun HomeTags_topright_btn_tag_is_consistent() {
    val topRightBtn = HomeTags.TopRightBtn
    assertTrue(topRightBtn.contains("topright"))
    assertTrue(topRightBtn.contains("btn"))
  }

  @Test
  fun HomeTags_message_field_tag_is_consistent() {
    val messageField = HomeTags.MessageField
    assertTrue(messageField.contains("message"))
    assertTrue(messageField.contains("field"))
  }

  @Test
  fun HomeTags_send_btn_tag_is_consistent() {
    val sendBtn = HomeTags.SendBtn
    assertTrue(sendBtn.contains("send"))
    assertTrue(sendBtn.contains("btn"))
  }
}
