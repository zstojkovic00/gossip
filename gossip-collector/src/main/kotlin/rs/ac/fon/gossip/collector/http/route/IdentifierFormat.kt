package rs.ac.fon.gossip.collector.http.route

enum class IdentifierFormat(val placeholder: String, private val format: Regex) {
    OBJECT_ID("{objectId}", Regex("[0-9a-fA-F]{24}")),
    UUID("{uuid}", Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")),
    NUMBER("{number}", Regex("[0-9]+"));

    companion object {
        fun detect(part: String): IdentifierFormat? = entries.firstOrNull { it.format.matches(part) }
    }
}
