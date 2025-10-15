package com.android.euler.auth

import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrosoftAuthTest {

    @Test
    fun `MicrosoftAuth object should be accessible`() {
        assertNotNull("MicrosoftAuth object should not be null", MicrosoftAuth)
    }

    @Test
    fun `MicrosoftAuth should have signIn method`() {
        // Test that the signIn method exists and is callable
        assertTrue(
            "MicrosoftAuth should have signIn method",
            MicrosoftAuth::class.java.methods.any { it.name == "signIn" }
        )
    }
}
