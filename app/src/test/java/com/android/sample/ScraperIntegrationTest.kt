package com.android.sample

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.min


//just a placeholder for now to compile code.
object Scraper {
    fun parse(html: String): String {
        return "PLACEHOLDER"
    }
}


/**
 * Integration tests for the EPFL scraper.
 * Verifies that the scraper output contains correct content and structure
 * compared to expected reference text.
 */
class ScraperIntegrationTest {

    private fun loadResource(path: String): String =
        File("src/test/resources/$path").readText()


    // --- Utility: measure Jaccard similarity between expected and actual texts ---
    private fun jaccardSimilarity(a: String, b: String): Double {
        val setA = a.lowercase().split("\\W+".toRegex()).toSet()
        val setB = b.lowercase().split("\\W+".toRegex()).toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        return setA.intersect(setB).size.toDouble() / setA.union(setB).size.toDouble()
    }

    // --- Shared validation method ---
    private fun validateScraper(htmlFile: String, expectedFile: String, keyPhrases: List<String>) {
        val html = loadResource("html/$htmlFile")
        val expected = loadResource("expected/$expectedFile")
        val actual = Scraper.parse(html) // we have to replace this with actual scraper call


        assertTrue("Output should not be empty", actual.isNotEmpty())
        assertFalse("Output should not contain HTML tags", Regex("<[^>]+>").containsMatchIn(actual))

        val wordCount = actual.split("\\s+".toRegex()).size
        assertTrue("Output should contain enough words", wordCount > 100)

        val similarity = jaccardSimilarity(expected, actual)
        println("Similarity for $htmlFile: $similarity")
        assertTrue("Scraper output should be at least 0.7 similar to expected text", similarity > 0.7)

        keyPhrases.forEach { phrase ->
            assertTrue("Output should contain keyword '$phrase'", actual.contains(phrase, ignoreCase = true))
        }
    }

    // --- Individual tests ---

    @Test
    fun testEpflAdmissionPage() {
        validateScraper(
            htmlFile = "epfl_admission_criteria.html",
            expectedFile = "epfl_admission_criteria.txt",
            keyPhrases = listOf(
                "Admission criteria",
                "Swiss maturity certificate",
                "foreign secondary school diploma",
                "entrance exam"
            )
        )
    }

    @Test
    fun testCs311Page() {
        validateScraper(
            htmlFile = "cs311.html",
            expectedFile = "cs311.txt",
            keyPhrases = listOf(
                "Software Enterprise",
                "project management",
                "agile development",
                "teamwork"
            )
        )
    }
}
