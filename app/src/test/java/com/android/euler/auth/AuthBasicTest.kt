package com.android.euler.auth

import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthBasicTest {

    @Test
    fun `MicrosoftAuth should be accessible`() {
        assertNotNull("MicrosoftAuth should not be null", MicrosoftAuth)
    }

    @Test
    fun `MicrosoftAuth should be an object`() {
        val instance1 = MicrosoftAuth
        val instance2 = MicrosoftAuth
        assertSame("Should be the same instance", instance1, instance2)
    }

    @Test
    fun `MicrosoftAuth should have expected class name`() {
        assertEquals("MicrosoftAuth", MicrosoftAuth::class.java.simpleName)
    }
}
