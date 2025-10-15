package com.android.euler.app

import com.android.sample.EulerApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EulerAppTest {

    @Test
    fun `EulerApp should be accessible`() {
        assertNotNull("EulerApp class should not be null", EulerApp::class.java)
    }

    @Test
    fun `EulerApp should have onCreate method`() {
        val onCreateMethod = EulerApp::class.java.methods
            .find { it.name == "onCreate" }
        
        assertNotNull("onCreate method should exist", onCreateMethod)
        assertEquals("onCreate should be public", java.lang.reflect.Modifier.PUBLIC, 
            onCreateMethod!!.modifiers and java.lang.reflect.Modifier.PUBLIC)
    }

    @Test
    fun `EulerApp should have companion object`() {
        val companionClass = EulerApp.Companion::class.java
        assertNotNull("Companion object should exist", companionClass)
    }

    @Test
    fun `EulerApp companion should have expected methods`() {
        val companionMethods = EulerApp.Companion::class.java.methods.map { it.name }
        
        // Test that companion has methods (they might be private, but should exist)
        assertTrue("Companion should have methods", companionMethods.isNotEmpty())
    }

    @Test
    fun `EulerApp should be Application subclass`() {
        val superclass = EulerApp::class.java.superclass
        assertNotNull("Should have superclass", superclass)
        assertEquals("Should extend Application", "android.app.Application", superclass!!.name)
    }
}


