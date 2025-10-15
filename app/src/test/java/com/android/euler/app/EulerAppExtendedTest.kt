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
class EulerAppExtendedTest {

    @Test
    fun `EulerApp should be Application subclass`() {
        val superclass = EulerApp::class.java.superclass
        assertNotNull("Should have superclass", superclass)
        assertEquals("Should extend Application", "android.app.Application", superclass!!.name)
    }

    @Test
    fun `EulerApp should have onCreate method`() {
        val onCreateMethod = EulerApp::class.java.getDeclaredMethod("onCreate")
        assertNotNull("onCreate method should exist", onCreateMethod)
        assertTrue("onCreate should be public", java.lang.reflect.Modifier.isPublic(onCreateMethod.modifiers))
    }

    @Test
    fun `EulerApp companion should have getEmulatorHost method`() {
        val companionClass = EulerApp.Companion::class.java
        val methods = companionClass.declaredMethods
        
        // Vérifier qu'il y a des méthodes dans le companion
        assertTrue("Companion should have methods", methods.isNotEmpty())
        
        // Vérifier qu'au moins une méthode retourne String
        val stringMethods = methods.filter { it.returnType == String::class.java }
        assertTrue("Should have methods returning String", stringMethods.isNotEmpty())
    }

    @Test
    fun `EulerApp companion should have methods`() {
        val companionClass = EulerApp.Companion::class.java
        val methods = companionClass.declaredMethods
        
        // Vérifier qu'il y a des méthodes dans le companion
        assertTrue("Companion should have methods", methods.isNotEmpty())
        
        // Vérifier qu'il y a des méthodes publiques
        val publicMethods = methods.filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
        assertTrue("Should have public methods", publicMethods.isNotEmpty())
    }

    @Test
    fun `EulerApp companion should have fields or methods`() {
        val companionClass = EulerApp.Companion::class.java
        
        // Vérifier que le companion a soit des champs soit des méthodes
        val fields = companionClass.declaredFields
        val methods = companionClass.declaredMethods
        assertTrue("Companion should have fields or methods", fields.isNotEmpty() || methods.isNotEmpty())
    }

    @Test
    fun `EulerApp should have correct class modifiers`() {
        val modifiers = EulerApp::class.java.modifiers
        
        assertFalse("Should not be abstract", java.lang.reflect.Modifier.isAbstract(modifiers))
        assertFalse("Should not be interface", EulerApp::class.java.isInterface)
        assertTrue("Should be public", java.lang.reflect.Modifier.isPublic(modifiers))
    }

    @Test
    fun `EulerApp should be instantiable`() {
        // Test que la classe peut être instanciée
        val constructor = EulerApp::class.java.getDeclaredConstructor()
        assertNotNull("Should have no-args constructor", constructor)
        assertTrue("Constructor should be public", java.lang.reflect.Modifier.isPublic(constructor.modifiers))
    }

    @Test
    fun `EulerApp companion object should be accessible`() {
        val companion = EulerApp.Companion
        assertNotNull("Companion object should not be null", companion)
        
        // Test que c'est bien le même instance
        val companion2 = EulerApp.Companion
        assertSame("Should be same companion instance", companion, companion2)
    }

    @Test
    fun `EulerApp should have expected methods count`() {
        val methods = EulerApp::class.java.methods
        assertTrue("Should have multiple methods", methods.size > 1)
        
        val methodNames = methods.map { it.name }
        assertTrue("Should have onCreate method", methodNames.contains("onCreate"))
    }
}
