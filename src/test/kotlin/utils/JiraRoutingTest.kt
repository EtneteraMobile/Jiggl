package utils

import kotlin.test.Test
import kotlin.test.assertEquals

class JiraRoutingTest {

    private val default = "https://default.atlassian.net"
    private val servers = arrayOf(
        "ABC" to "https://abc.atlassian.net",
        "INT, OPS" to "https://internal.jira.example.com",
    )

    @Test
    fun matches_single_project_key() {
        assertEquals("https://abc.atlassian.net", resolveJiraUrl("ABC-123", servers, default))
    }

    @Test
    fun matches_key_from_comma_separated_list() {
        assertEquals("https://internal.jira.example.com", resolveJiraUrl("OPS-7", servers, default))
        assertEquals("https://internal.jira.example.com", resolveJiraUrl("INT-42", servers, default))
    }

    @Test
    fun falls_back_to_default_when_no_match() {
        assertEquals(default, resolveJiraUrl("XYZ-1", servers, default))
    }

    @Test
    fun matching_is_case_insensitive() {
        assertEquals("https://abc.atlassian.net", resolveJiraUrl("abc-123", servers, default))
        val lowercaseServers = arrayOf("abc" to "https://abc.atlassian.net")
        assertEquals("https://abc.atlassian.net", resolveJiraUrl("ABC-123", lowercaseServers, default))
    }

    @Test
    fun ignores_whitespace_around_keys() {
        val spaced = arrayOf(" ABC , INT " to "https://abc.atlassian.net")
        assertEquals("https://abc.atlassian.net", resolveJiraUrl("INT-1", spaced, default))
    }

    @Test
    fun prefix_must_match_whole_project_key() {
        assertEquals(default, resolveJiraUrl("ABCD-1", servers, default))
        assertEquals(default, resolveJiraUrl("AB-1", servers, default))
    }

    @Test
    fun empty_key_list_never_matches() {
        val migrated = arrayOf("" to "https://legacy.jira.example.com")
        assertEquals(default, resolveJiraUrl("ABC-123", migrated, default))
    }

    @Test
    fun empty_or_keyless_issue_uses_default() {
        assertEquals(default, resolveJiraUrl("", servers, default))
        assertEquals(default, resolveJiraUrl("-123", servers, default))
    }

    @Test
    fun first_matching_server_wins() {
        val overlapping = arrayOf(
            "ABC" to "https://first.example.com",
            "ABC" to "https://second.example.com",
        )
        assertEquals("https://first.example.com", resolveJiraUrl("ABC-1", overlapping, default))
    }
}
