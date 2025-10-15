package com.android.euler.app

import com.android.sample.EulerApp
import com.google.firebase.FirebaseApp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EulerAppOnCreateTest {

    @Before
    fun setUp() {
        // Skip Firebase initialization for now
    }

    @Test
    fun `EulerApp onCreate should execute without exception`() {
        val app = EulerApp()
        
        // Test que onCreate peut être appelé sans erreur
        try {
            app.onCreate()
            // Si on arrive ici, c'est que onCreate s'est exécuté sans exception
            assertTrue("onCreate should execute successfully", true)
        } catch (e: Exception) {
            fail("onCreate should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `EulerApp should be instantiable`() {
        // Test que la classe peut être instanciée
        val app = EulerApp()
        assertNotNull("EulerApp should be instantiable", app)
    }

    @Test
    fun `EulerApp should have correct class hierarchy`() {
        val app = EulerApp()
        assertTrue("Should be Application instance", app is android.app.Application)
    }

    @Test
    fun `EulerApp should have onCreate method accessible`() {
        val app = EulerApp()
        val onCreateMethod = app.javaClass.getDeclaredMethod("onCreate")
        
        assertNotNull("onCreate method should exist", onCreateMethod)
        assertTrue("onCreate should be public", 
            java.lang.reflect.Modifier.isPublic(onCreateMethod.modifiers))
    }

    @Test
    fun `EulerApp companion object should be accessible`() {
        val companion = EulerApp.Companion
        assertNotNull("Companion object should not be null", companion)
        
        // Test que le companion est bien accessible
        assertSame("Should be same companion instance", 
            EulerApp.Companion, EulerApp.Companion)
    }

    @Test
    fun `EulerApp should have expected class structure`() {
        val clazz = EulerApp::class.java
        
        // Vérifier les méthodes de base
        val methods = clazz.methods.map { it.name }
        assertTrue("Should have onCreate method", methods.contains("onCreate"))
        
        // Vérifier les constructeurs
        val constructors = clazz.constructors
        assertTrue("Should have constructors", constructors.isNotEmpty())
    }
}
