package com.quare.blitzsplit.core.provider.analytics

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/*
 * Why: what the compiler can't check of the events the features declare (docs/analytics/README.md): each name follows
 * GA4's rules, has its row in the README's event catalog and every row an event behind it, and each
 * EventAnalytics.Manual name is really sent by a trackEvent call of its module.
 */
class AnalyticsCatalogTest {
    private val repoRoot: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() && File(it, "docs/analytics/README.md").exists() }
    private val eventNameRegex = Regex("""[a-z][a-z0-9_]{0,39}""")
    private val automaticRegex = Regex("""EventAnalytics\.Automatic\(\s*(?:name\s*=\s*)?"([^"]+)"""")
    private val literalRegex = Regex(""""([^"]+)"""")
    private val catalogRowRegex = Regex("""^\|\s*`([^`]+)`\s*\|""", RegexOption.MULTILINE)

    @Test
    fun `GIVEN every declared event WHEN checking its name THEN it is snake case within the 40 letters GA4 takes`() {
        // Given
        val names = findDeclaredNames()

        // When
        val violations = names.filterNot { name -> eventNameRegex.matches(name) }

        // Then
        if (violations.isNotEmpty()) fail("Event names GA4 won't take:\n" + violations.joinToString("\n"))
    }

    @Test
    fun `GIVEN every declared event WHEN looking it up in the README THEN it has a row`() {
        // Given
        val catalog = readCatalog()

        // When
        val violations = findDeclaredNames().filterNot { name -> name in catalog }

        // Then
        if (violations.isNotEmpty()) {
            fail("Events missing from docs/analytics/README.md:\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun `GIVEN every row of the README catalog WHEN looking for its event THEN a feature declares it`() {
        // Given
        val declared = findDeclaredNames()

        // When
        val violations = readCatalog().filterNot { name -> name in declared }

        // Then
        if (violations.isNotEmpty()) fail("Catalog rows no feature declares:\n" + violations.joinToString("\n"))
    }

    @Test
    fun `GIVEN every Manual event WHEN searching its module THEN a trackEvent call sends it`() {
        // Given
        val declarations = findDeclarationFiles()

        // When
        val violations = declarations.flatMap { file ->
            val sources = file
                .moduleRoot()
                .kotlinSources()
                .filter { it != file }
                .map { it.readText() }
                .toList()
            findManualNames(file.readText())
                .filterNot { name ->
                    val call = Regex("""trackEvent\(\s*(?:name\s*=\s*)?"${Regex.escape(name)}"""")
                    sources.any { source -> call.containsMatchIn(source) }
                }.map { name -> "${file.relativeTo(repoRoot)}: Manual(\"$name\") has no trackEvent call sending it" }
        }

        // Then
        if (violations.isNotEmpty()) fail("Manual events nobody sends:\n" + violations.joinToString("\n"))
    }

    private fun findDeclaredNames(): Set<String> = findDeclarationFiles()
        .flatMap { file -> findAutomaticNames(file.readText()) + findManualNames(file.readText()) }
        .toSortedSet()

    private fun findDeclarationFiles(): List<File> = File(repoRoot, "feature")
        .kotlinSources()
        .filter { file -> file.readText().contains("EventAnalytics.") }
        .toList()

    private fun findAutomaticNames(text: String): Set<String> =
        automaticRegex.findAll(text).map { it.groupValues[1] }.toSet()

    private fun findManualNames(text: String): Set<String> = findCallArguments(text, "EventAnalytics.Manual(")
        .flatMap { arguments -> literalRegex.findAll(arguments).map { it.groupValues[1] } }
        .toSet()

    // Why: the event names of the catalog's table, the rows under "## Events" up to the next section.
    private fun readCatalog(): Set<String> {
        val readme = File(repoRoot, "docs/analytics/README.md").readText()
        val start = readme.indexOf(CATALOG_HEADING).takeIf { it >= 0 }
            ?: fail("\"$CATALOG_HEADING\" not found in docs/analytics/README.md")
        val end =
            readme.indexOf("\n## ", startIndex = start + CATALOG_HEADING.length).takeIf { it >= 0 } ?: readme.length
        return catalogRowRegex.findAll(readme.substring(start, end)).map { it.groupValues[1] }.toSet()
    }

    private fun findCallArguments(
        text: String,
        marker: String,
    ): List<String> {
        val arguments = mutableListOf<String>()
        var index = text.indexOf(marker)
        while (index != -1) {
            var depth = 1
            var end = index + marker.length
            while (end < text.length && depth > 0) {
                when (text[end]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                end++
            }
            arguments += text.substring(index + marker.length, end - 1)
            index = text.indexOf(marker, end)
        }
        return arguments
    }

    private fun File.kotlinSources(): Sequence<File> = walkTopDown()
        .onEnter { directory -> directory.name != "build" }
        .filter { it.isFile && it.extension == "kt" }

    private fun File.moduleRoot(): File = generateSequence(parentFile) { it.parentFile }
        .first { File(it, "build.gradle.kts").exists() }

    private companion object {
        const val CATALOG_HEADING = "## Events"
    }
}
