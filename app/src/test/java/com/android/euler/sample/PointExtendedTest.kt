package com.android.euler.sample

import com.android.sample.Point
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PointExtendedTest {

    @Test
    fun `Point should handle zero coordinates`() {
        // Test des coordonnées zéro
        val point = Point(0.0, 0.0)
        assertEquals("X should be 0", 0.0, point.x, 0.001)
        assertEquals("Y should be 0", 0.0, point.y, 0.001)

        // Test de distance avec point zéro
        val anotherPoint = Point(0.0, 0.0)
        val distance = point.distanceTo(anotherPoint)
        assertEquals("Distance should be 0", 0.0, distance, 0.001)
    }

    @Test
    fun `Point should handle negative coordinates`() {
        // Test des coordonnées négatives
        val point1 = Point(-1.0, -2.0)
        val point2 = Point(-3.0, -4.0)

        assertEquals("X1 should be -1", -1.0, point1.x, 0.001)
        assertEquals("Y1 should be -2", -2.0, point1.y, 0.001)
        assertEquals("X2 should be -3", -3.0, point2.x, 0.001)
        assertEquals("Y2 should be -4", -4.0, point2.y, 0.001)

        // Test de distance avec coordonnées négatives
        val distance = point1.distanceTo(point2)
        assertEquals("Distance should be 2.828", 2.828, distance, 0.01)
    }

    @Test
    fun `Point should handle decimal coordinates`() {
        // Test des coordonnées décimales
        val point1 = Point(1.5, 2.7)
        val point2 = Point(3.2, 4.8)

        assertEquals("X1 should be 1.5", 1.5, point1.x, 0.001)
        assertEquals("Y1 should be 2.7", 2.7, point1.y, 0.001)
        assertEquals("X2 should be 3.2", 3.2, point2.x, 0.001)
        assertEquals("Y2 should be 4.8", 4.8, point2.y, 0.001)

        // Test de distance avec coordonnées décimales
        val distance = point1.distanceTo(point2)
        assertTrue("Distance should be approximately 2.69", distance > 2.6 && distance < 2.8)
    }

    @Test
    fun `Point should handle large coordinates`() {
        // Test des coordonnées grandes
        val point1 = Point(1000.0, 2000.0)
        val point2 = Point(3000.0, 4000.0)

        assertEquals("X1 should be 1000", 1000.0, point1.x, 0.001)
        assertEquals("Y1 should be 2000", 2000.0, point1.y, 0.001)
        assertEquals("X2 should be 3000", 3000.0, point2.x, 0.001)
        assertEquals("Y2 should be 4000", 4000.0, point2.y, 0.001)

        // Test de distance avec coordonnées grandes
        val distance = point1.distanceTo(point2)
        assertEquals("Distance should be 2828.43", 2828.43, distance, 0.01)
    }

    @Test
    fun `Point should handle same coordinates`() {
        // Test des mêmes coordonnées
        val point1 = Point(5.0, 10.0)
        val point2 = Point(5.0, 10.0)

        // Test de distance avec mêmes coordonnées
        val distance = point1.distanceTo(point2)
        assertEquals("Distance should be 0", 0.0, distance, 0.001)
    }

    @Test
    fun `Point should handle horizontal distance`() {
        // Test de distance horizontale
        val point1 = Point(0.0, 0.0)
        val point2 = Point(5.0, 0.0)

        val distance = point1.distanceTo(point2)
        assertEquals("Horizontal distance should be 5", 5.0, distance, 0.001)
    }

    @Test
    fun `Point should handle vertical distance`() {
        // Test de distance verticale
        val point1 = Point(0.0, 0.0)
        val point2 = Point(0.0, 5.0)

        val distance = point1.distanceTo(point2)
        assertEquals("Vertical distance should be 5", 5.0, distance, 0.001)
    }

    @Test
    fun `Point should handle diagonal distance`() {
        // Test de distance diagonale
        val point1 = Point(0.0, 0.0)
        val point2 = Point(3.0, 4.0)

        val distance = point1.distanceTo(point2)
        assertEquals("Diagonal distance should be 5", 5.0, distance, 0.001)
    }

    @Test
    fun `Point should handle precision`() {
        // Test de précision
        val point1 = Point(1.0000001, 2.0000001)
        val point2 = Point(1.0000002, 2.0000002)

        val distance = point1.distanceTo(point2)
        assertTrue("Distance should be very small", distance < 0.001)
    }

    @Test
    fun `Point should handle edge cases`() {
        // Test des cas limites
        val point1 = Point(Double.MIN_VALUE, Double.MAX_VALUE)
        val point2 = Point(Double.MAX_VALUE, Double.MIN_VALUE)

        assertTrue("Point1 X should be minimal", point1.x == Double.MIN_VALUE)
        assertTrue("Point1 Y should be maximal", point1.y == Double.MAX_VALUE)
        assertTrue("Point2 X should be maximal", point2.x == Double.MAX_VALUE)
        assertTrue("Point2 Y should be minimal", point2.y == Double.MIN_VALUE)

        // Test de distance avec cas limites
        val distance = point1.distanceTo(point2)
        assertTrue("Distance should be calculated", distance >= 0.0)
    }

    @Test
    fun `Point should handle multiple distance calculations`() {
        // Test de calculs multiples de distance
        val points =
            listOf(
                Point(0.0, 0.0),
                Point(1.0, 0.0),
                Point(2.0, 0.0),
                Point(3.0, 0.0),
                Point(4.0, 0.0)
            )

        // Calculer les distances entre points consécutifs
        for (i in 0 until points.size - 1) {
            val distance = points[i].distanceTo(points[i + 1])
            assertEquals("Distance should be 1", 1.0, distance, 0.001)
        }
    }

    @Test
    fun `Point should handle distance symmetry`() {
        // Test de symétrie de la distance
        val point1 = Point(1.0, 2.0)
        val point2 = Point(3.0, 4.0)

        val distance1 = point1.distanceTo(point2)
        val distance2 = point2.distanceTo(point1)

        assertEquals("Distances should be symmetric", distance1, distance2, 0.001)
    }

    @Test
    fun `Point should handle triangle inequality`() {
        // Test de l'inégalité triangulaire
        val point1 = Point(0.0, 0.0)
        val point2 = Point(3.0, 0.0)
        val point3 = Point(3.0, 4.0)

        val distance12 = point1.distanceTo(point2)
        val distance23 = point2.distanceTo(point3)
        val distance13 = point1.distanceTo(point3)

        assertTrue("Triangle inequality should hold", distance13 <= distance12 + distance23)
    }

    @Test
    fun `Point should handle coordinate access`() {
        // Test d'accès aux coordonnées
        val point = Point(7.5, 9.3)

        // Vérifier l'accès direct
        assertEquals("Direct X access", 7.5, point.x, 0.001)
        assertEquals("Direct Y access", 9.3, point.y, 0.001)

        // Vérifier que les coordonnées sont immutables (conceptuellement)
        assertTrue("X should be accessible", point.x is Double)
        assertTrue("Y should be accessible", point.y is Double)
    }

    @Test
    fun `Point should handle mathematical operations`() {
        // Test d'opérations mathématiques
        val point1 = Point(2.0, 3.0)
        val point2 = Point(4.0, 5.0)

        // Test de distance avec opérations mathématiques
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        val expectedDistance = Math.sqrt(dx * dx + dy * dy)
        val actualDistance = point1.distanceTo(point2)

        assertEquals(
            "Distance calculation should be correct",
            expectedDistance,
            actualDistance,
            0.001
        )
    }

    @Test
    fun `Point should handle boundary conditions`() {
        // Test des conditions aux limites
        val boundaryPoints =
            listOf(
                Point(0.0, 0.0),
                Point(-1.0, 0.0),
                Point(0.0, -1.0),
                Point(1.0, 1.0),
                Point(-1.0, -1.0)
            )

        // Calculer toutes les distances possibles
        for (i in boundaryPoints.indices) {
            for (j in boundaryPoints.indices) {
                val distance = boundaryPoints[i].distanceTo(boundaryPoints[j])
                assertTrue("Distance should be non-negative", distance >= 0.0)
            }
        }
    }

    @Test
    fun `Point should handle performance with many calculations`() {
        // Test de performance avec beaucoup de calculs
        val startTime = System.currentTimeMillis()

        val points = mutableListOf<Point>()
        for (i in 0..1000) {
            points.add(Point(i.toDouble(), i.toDouble()))
        }

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += points[i].distanceTo(points[i + 1])
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        assertTrue("Performance should be acceptable", duration < 1000) // Moins d'1 seconde
        assertTrue("Total distance should be calculated", totalDistance > 0.0)
    }
}
