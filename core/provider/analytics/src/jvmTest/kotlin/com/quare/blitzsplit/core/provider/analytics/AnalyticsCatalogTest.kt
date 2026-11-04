package com.quare.blitzsplit.core.provider.analytics

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/*
 * Why: what the compiler can't check of the events the features declare (docs/analytics/README.md): each name and param
 * follows GA4's rules and is a literal the test can read, each event has its row in the README's catalog and every row
 * an event behind it, each EventAnalytics.Manual name is sent by a trackEvent call of its module, and every trackEvent
 * call of a feature sends a Manual name its module declares.
 */
class AnalyticsCatalogTest {
    private val repoRoot: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() && File(it, "docs/analytics/README.md").exists() }
    private val gaNameRegex = Regex("""[a-z][a-z0-9_]{0,39}""")
    private val leadingLiteralRegex = Regex("""^\s*(?:name\s*=\s*)?"([^"]+)"""")
    private val literalRegex = Regex(""""([^"]+)"""")
    private val paramKeyRegex = Regex(""""([^"]+)"\s+to\b""")
    private val nestedImportRegex = Regex("""import [\w.]*\.EventAnalytics\.(?:\w+|\*)""")
    private val catalogRowRegex = Regex("""^\|\s*`([^`]+)`\s*\|""", RegexOption.MULTILINE)

    private val reservedNames = setOf(
        "ad_activeview",
        "ad_click",
        "ad_exposure",
        "ad_query",
        "ad_reward",
        "adunit_exposure",
        "app_background",
        "app_clear_data",
        "app_exception",
        "app_remove",
        "app_store_refund",
        "app_store_subscription_cancel",
        "app_store_subscription_convert",
        "app_store_subscription_renew",
        "app_update",
        "app_upgrade",
        "dynamic_link_app_open",
        "dynamic_link_app_update",
        "dynamic_link_first_open",
        "error",
        "first_open",
        "first_visit",
        "in_app_purchase",
        "notification_dismiss",
        "notification_foreground",
        "notification_open",
        "notification_receive",
        "os_update",
        "session_start",
        "session_start_with_rollout",
        "user_engagement",
        "screen_view",
    )
    private val reservedPrefixes = listOf("firebase_", "google_", "ga_")

    // Why: read once for every test, comments left out, so a commented-out declaration declares nothing.
    private val sources: List<Source> by lazy {
        listOf("feature", "core").flatMap { root ->
            File(repoRoot, root).mainSources().map { file ->
                Source(
                    file = file,
                    module = file.moduleRoot(),
                    isFeature = root == "feature",
                    text = file.readCode(),
                )
            }
        }
    }

    @Test
    fun `GIVEN every declared event WHEN checking its name THEN it is a snake case name GA4 takes and keeps`() {
        // Given
        val names = findDeclaredNames()

        // When
        val violations = names.filterNot(::isGaName)

        // Then
        if (violations.isNotEmpty()) fail("Event names GA4 won't take:\n" + violations.joinToString("\n"))
    }

    @Test
    fun `GIVEN every declaration WHEN reading its name THEN it is a prefixed literal the test can read`() {
        // Given
        val declarations = sources.filter { it.text.contains("EventAnalytics") }

        // When
        val violations = declarations.flatMap { source ->
            val automatic = findCalls(source.text, "EventAnalytics.Automatic(")
                .filterNot { arguments -> leadingLiteralRegex.containsMatchIn(arguments) }
            val manual = findCalls(source.text, "EventAnalytics.Manual(")
                .filterNot { arguments -> literalRegex.containsMatchIn(arguments) }
            val nestedImports = nestedImportRegex.findAll(source.text).map { it.value }.toList()
            (automatic + manual + nestedImports).map { found -> "${source.path}: $found" }
        }

        // Then
        if (violations.isNotEmpty()) {
            fail("Event names the test can't read (a literal after EventAnalytics.):\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun `GIVEN every trackEvent call of a feature WHEN reading its name THEN it is a literal`() {
        // Given
        val features = sources.filter { it.isFeature }

        // When
        val violations = features.flatMap { source ->
            findCalls(source.text, "trackEvent(")
                .filterNot { arguments -> leadingLiteralRegex.containsMatchIn(arguments) }
                .map { arguments -> "${source.path}: trackEvent($arguments)" }
        }

        // Then
        if (violations.isNotEmpty()) fail("trackEvent calls with no literal name:\n" + violations.joinToString("\n"))
    }

    @Test
    fun `GIVEN every event sent WHEN reading its params THEN each is a name GA4 takes and they are 25 at most`() {
        // Given
        val calls = sources.filter { it.isFeature || it.text.contains("EventAnalytics.Automatic(") }.flatMap { source ->
            (findCalls(source.text, "EventAnalytics.Automatic(") + findCalls(source.text, "trackEvent("))
                .map { arguments -> source to arguments }
        }

        // When
        val violations = calls.flatMap { (source, arguments) ->
            val keys = paramKeyRegex.findAll(arguments).map { it.groupValues[1] }.toList()
            val badKeys = keys.filterNot(::isGaName).map { key -> "${source.path}: param \"$key\"" }
            val tooMany = if (keys.size > MAX_PARAMS) listOf("${source.path}: ${keys.size} params") else emptyList()
            badKeys + tooMany
        }

        // Then
        if (violations.isNotEmpty()) fail("Params GA4 won't take:\n" + violations.joinToString("\n"))
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
        val modules = sources.groupBy { it.module }

        // When
        val violations = modules.flatMap { (_, moduleSources) ->
            val sent = moduleSources.flatMap { findSentNames(it.text) }.toSet()
            moduleSources.flatMap { source ->
                findManualNames(source.text)
                    .filterNot { name -> name in sent }
                    .map { name -> "${source.path}: Manual(\"$name\") has no trackEvent call sending it" }
            }
        }

        // Then
        if (violations.isNotEmpty()) fail("Manual events nobody sends:\n" + violations.joinToString("\n"))
    }

    @Test
    fun `GIVEN every trackEvent call WHEN looking for its event THEN a Manual of its module declares it`() {
        // Given
        val modules = sources.filter { it.isFeature }.groupBy { it.module }

        // When
        val violations = modules.flatMap { (module, moduleSources) ->
            val manual = moduleSources.flatMap { findManualNames(it.text) }.toSet()
            moduleSources
                .flatMap { findSentNames(it.text) }
                .filterNot { name -> name in manual }
                .map { name -> "${module.relativeTo(repoRoot)}: trackEvent(\"$name\") no Manual declares" }
        }

        // Then
        if (violations.isNotEmpty()) fail("Events sent without a declaration:\n" + violations.joinToString("\n"))
    }

    private fun isGaName(name: String): Boolean =
        gaNameRegex.matches(name) && name !in reservedNames && reservedPrefixes.none(name::startsWith)

    private fun findDeclaredNames(): Set<String> = sources
        .flatMap { source -> findAutomaticNames(source.text) + findManualNames(source.text) }
        .toSortedSet()

    private fun findAutomaticNames(text: String): List<String> = findCalls(text, "EventAnalytics.Automatic(")
        .mapNotNull { arguments -> leadingLiteralRegex.find(arguments)?.groupValues?.get(1) }

    private fun findManualNames(text: String): List<String> = findCalls(text, "EventAnalytics.Manual(")
        .flatMap { arguments -> literalRegex.findAll(arguments).map { it.groupValues[1] } }

    private fun findSentNames(text: String): List<String> = findCalls(text, "trackEvent(")
        .mapNotNull { arguments -> leadingLiteralRegex.find(arguments)?.groupValues?.get(1) }

    // Why: the event names of the catalog's table, the rows under "## Events" up to the next section.
    private fun readCatalog(): Set<String> {
        val readme = File(repoRoot, "docs/analytics/README.md").readText()
        val start = readme.indexOf(CATALOG_HEADING).takeIf { it >= 0 }
            ?: fail("\"$CATALOG_HEADING\" not found in docs/analytics/README.md")
        val end =
            readme.indexOf("\n## ", startIndex = start + CATALOG_HEADING.length).takeIf { it >= 0 } ?: readme.length
        return catalogRowRegex.findAll(readme.substring(start, end)).map { it.groupValues[1] }.toSet()
    }

    /*
     * Why: the arguments of each call to marker, up to its closing parenthesis, skipping those inside a string; a call
     * left open runs to the end of the text, never past it. Only a call of that very name counts, not one ending in it.
     */
    private fun findCalls(
        text: String,
        marker: String,
    ): List<String> {
        val calls = mutableListOf<String>()
        var index = text.indexOf(marker)
        while (index != -1) {
            val isWholeName = index == 0 || (!text[index - 1].isLetterOrDigit() && text[index - 1] != '_')
            val start = index + marker.length
            var end = start
            var depth = 1
            var inString = false
            while (end < text.length && depth > 0) {
                val char = text[end]
                when {
                    char == '\\' && inString -> end++
                    char == '"' -> inString = !inString
                    !inString && char == '(' -> depth++
                    !inString && char == ')' -> depth--
                }
                end++
            }
            if (isWholeName) calls += text.substring(start, if (depth == 0) end - 1 else end)
            index = text.indexOf(marker, end.coerceAtLeast(start))
        }
        return calls
    }

    // Why: the source without its comments, its strings kept, so a URL's "//" stays in the string it is in.
    private fun File.readCode(): String {
        val text = readText()
        val code = StringBuilder(text.length)
        var index = 0
        var inString = false
        while (index < text.length) {
            val char = text[index]
            val next = text.getOrNull(index + 1)
            when {
                inString -> {
                    code.append(char)
                    if (char == '\\' &&
                        next != null
                    ) {
                        code.append(next).also { index++ }
                    } else if (char == '"') {
                        inString = false
                    }
                }

                char == '"' -> code.append(char).also { inString = true }

                char == '/' && next == '/' -> index = (text.indexOf('\n', index).takeIf { it >= 0 } ?: text.length) - 1

                char == '/' && next == '*' ->
                    index =
                        (text.indexOf("*/", index + 2).takeIf { it >= 0 } ?: text.length) + 1

                else -> code.append(char)
            }
            index++
        }
        return code.toString()
    }

    // Why: the production code only, so a test's fake event neither declares an event nor keeps a row alive.
    private fun File.mainSources(): Sequence<File> = walkTopDown()
        .onEnter { directory -> directory.name != "build" && !directory.name.endsWith("Test") }
        .filter { it.isFile && it.extension == "kt" }

    private fun File.moduleRoot(): File = generateSequence(parentFile) { it.parentFile }
        .first { File(it, "build.gradle.kts").exists() }

    private inner class Source(
        val file: File,
        val module: File,
        val isFeature: Boolean,
        val text: String,
    ) {
        val path: String get() = file.relativeTo(repoRoot).path
    }

    private companion object {
        const val CATALOG_HEADING = "## Events"

        // Why: GA4 keeps at most 25 params of an event.
        const val MAX_PARAMS = 25
    }
}
