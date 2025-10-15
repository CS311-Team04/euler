package com.android.euler.sample

import com.android.sample.Point
import org.junit.Assert.*
import org.junit.Test

class SimpleDataTest {

    @Test
    fun `Point should have correct coordinates`() {
        val point = Point(3.0, 4.0)
        assertEquals(3.0, point.x, 0.001)
        assertEquals(4.0, point.y, 0.001)
    }

    @Test
    fun `distanceTo should calculate correct distance between two points`() {
        val point1 = Point(0.0, 0.0)
        val point2 = Point(3.0, 4.0)
        val distance = point1.distanceTo(point2)
        assertEquals(5.0, distance, 0.001) // 3-4-5 triangle
    }

    @Test
    fun `distanceTo should return 0 for same points`() {
        val point = Point(1.0, 2.0)
        val distance = point.distanceTo(point)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `distanceTo should be symmetric`() {
        val point1 = Point(1.0, 1.0)
        val point2 = Point(4.0, 5.0)
        val distance1 = point1.distanceTo(point2)
        val distance2 = point2.distanceTo(point1)
        assertEquals(distance1, distance2, 0.001)
    }

    @Test
    fun `distanceTo should handle negative coordinates`() {
        val point1 = Point(-1.0, -1.0)
        val point2 = Point(2.0, 3.0)
        val distance = point1.distanceTo(point2)
        assertEquals(5.0, distance, 0.001) // sqrt((2-(-1))² + (3-(-1))²) = sqrt(9+16) = 5
    }
}
