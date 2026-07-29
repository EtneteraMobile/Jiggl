package utils

/**
 * Resolves which Jira server an issue should be logged to.
 *
 * Each server entry pairs a comma-separated list of Jira project keys
 * (e.g. "ABC, INT") with a server url. The issue's project key (the part
 * before the first dash) is matched case-insensitively against those lists.
 * Entries with no project keys never match, so migrated legacy servers
 * stay dormant until the user assigns keys to them.
 *
 * @param issue Jira issue key, e.g. "ABC-123".
 * @param servers Configured servers as (project keys csv -> url).
 * @param defaultUrl Url used when no server matches.
 */
fun resolveJiraUrl(issue: String, servers: Array<Pair<String, String>>, defaultUrl: String): String {
    val projectKey = issue.substringBefore('-').trim().uppercase()
    if (projectKey.isEmpty()) return defaultUrl

    return servers.firstOrNull { server ->
        server.first.split(',').any { it.trim().uppercase() == projectKey }
    }?.second ?: defaultUrl
}
