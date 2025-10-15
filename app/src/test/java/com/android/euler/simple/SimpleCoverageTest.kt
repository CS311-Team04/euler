package com.android.euler.simple

import com.android.sample.Point
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SimpleCoverageTest {

    @Test
    fun `Point creation and basic operations`() {
        val point1 = Point(0.0, 0.0)
        val point2 = Point(3.0, 4.0)
        
        assertEquals(0.0, point1.x, 0.001)
        assertEquals(0.0, point1.y, 0.001)
        assertEquals(3.0, point2.x, 0.001)
        assertEquals(4.0, point2.y, 0.001)
        
        // Test distance calculation
        val distance = point1.distanceTo(point2)
        assertEquals(5.0, distance, 0.001) // 3-4-5 triangle
    }

    @Test
    fun `Point distance to itself is zero`() {
        val point = Point(10.0, 20.0)
        val distance = point.distanceTo(point)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `Point distance calculation with negative coordinates`() {
        val point1 = Point(-3.0, -4.0)
        val point2 = Point(0.0, 0.0)
        
        val distance = point1.distanceTo(point2)
        assertEquals(5.0, distance, 0.001)
    }
}
