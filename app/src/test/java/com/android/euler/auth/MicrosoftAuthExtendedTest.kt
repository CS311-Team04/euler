package com.android.euler.auth

import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrosoftAuthExtendedTest {

    @Test
    fun `MicrosoftAuth should have all expected methods`() {
        val methods = MicrosoftAuth::class.java.methods.map { it.name }

        assertTrue("Should have signIn method", methods.contains("signIn"))
        assertTrue("Should have multiple methods", methods.size > 1)

        // Test that we can access the class
        assertNotNull("MicrosoftAuth class should not be null", MicrosoftAuth::class.java)
    }

    @Test
    fun `MicrosoftAuth should be accessible as singleton`() {
        val instance1 = MicrosoftAuth
        val instance2 = MicrosoftAuth

        assertSame("Should be the same instance", instance1, instance2)
        assertNotNull("Instance should not be null", instance1)
    }

    @Test
    fun `MicrosoftAuth class should have correct modifiers`() {
        val modifiers = MicrosoftAuth::class.java.modifiers

        assertTrue("Should be final", java.lang.reflect.Modifier.isFinal(modifiers))
        assertFalse("Should not be abstract", java.lang.reflect.Modifier.isAbstract(modifiers))
        assertFalse("Should not be interface", MicrosoftAuth::class.java.isInterface)
    }

    @Test
    fun `MicrosoftAuth should have signIn method with correct signature`() {
        val signInMethod = MicrosoftAuth::class.java.methods.find { it.name == "signIn" }

        assertNotNull("signIn method should exist", signInMethod)
        assertEquals(
            "signIn should be public",
            java.lang.reflect.Modifier.PUBLIC,
            signInMethod!!.modifiers and java.lang.reflect.Modifier.PUBLIC
        )
    }
}
