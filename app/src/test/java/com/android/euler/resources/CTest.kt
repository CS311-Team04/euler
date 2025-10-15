package com.android.euler.resources

import com.android.sample.resources.C
import org.junit.Assert.*
import org.junit.Test

class CTest {

    @Test
    fun `Tag constants should have correct values`() {
        assertEquals("main_screen_greeting", C.Tag.greeting)
        assertEquals("second_screen_greeting", C.Tag.greeting_robo)
        assertEquals("main_screen_container", C.Tag.main_screen_container)
        assertEquals("second_screen_container", C.Tag.second_screen_container)
        assertEquals("screen_home", C.Tag.screenHome)
        assertEquals("screen_chat", C.Tag.screenChat)
        assertEquals("chat_input", C.Tag.chatInput)
        assertEquals("screen_settings", C.Tag.screenSettings)
    }

    @Test
    fun `Tag constants should not be empty`() {
        assertTrue("greeting should not be empty", C.Tag.greeting.isNotEmpty())
        assertTrue("greeting_robo should not be empty", C.Tag.greeting_robo.isNotEmpty())
        assertTrue(
            "main_screen_container should not be empty",
            C.Tag.main_screen_container.isNotEmpty()
        )
        assertTrue(
            "second_screen_container should not be empty",
            C.Tag.second_screen_container.isNotEmpty()
        )
        assertTrue("screenHome should not be empty", C.Tag.screenHome.isNotEmpty())
        assertTrue("screenChat should not be empty", C.Tag.screenChat.isNotEmpty())
        assertTrue("chatInput should not be empty", C.Tag.chatInput.isNotEmpty())
        assertTrue("screenSettings should not be empty", C.Tag.screenSettings.isNotEmpty())
    }
}
