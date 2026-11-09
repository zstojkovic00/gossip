package rs.ac.fon.gossip.collector.http.route


class FunctionalityResolver(patterns: List<RoutePattern>) {
    private val patterns = patterns.sortedByDescending { it.literalCount }

    init {
        val equivalent = patterns.groupBy { it.parts }.values.filter { it.size > 1 }
        require(equivalent.isEmpty()) {
            "Route patterns match the same paths: ${equivalent.map { group -> group.map { it.template } }}"
        }
    }

    fun resolve(path: String): String {
        if (!path.startsWith("/")) return path

        val parts = path.removePrefix("/").split("/")
        patterns.firstOrNull { it.matches(parts) }?.let { return it.template }

        return parts.joinToString("/", prefix = "/") { IdentifierFormat.detect(it)?.placeholder ?: it }
    }
}