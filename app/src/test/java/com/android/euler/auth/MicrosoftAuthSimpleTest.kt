package com.android.euler.auth

import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrosoftAuthSimpleTest {

    @Test
    fun `MicrosoftAuth object should be accessible`() {
        assertNotNull("MicrosoftAuth should not be null", MicrosoftAuth)
    }

    @Test
    fun `MicrosoftAuth should be singleton`() {
        val instance1 = MicrosoftAuth
        val instance2 = MicrosoftAuth
        assertSame("Should be same instance", instance1, instance2)
    }

    @Test
    fun `MicrosoftAuth should have signIn method`() {
        val hasSignIn = MicrosoftAuth::class.java.methods.any { it.name == "signIn" }
        assertTrue("Should have signIn method", hasSignIn)
    }

    @Test
    fun `MicrosoftAuth class should be final`() {
        val modifiers = MicrosoftAuth::class.java.modifiers
        assertTrue("Should be final", java.lang.reflect.Modifier.isFinal(modifiers))
    }

    @Test
    fun `MicrosoftAuth should have correct class name`() {
        assertEquals("MicrosoftAuth", MicrosoftAuth::class.java.simpleName)
    }
}
