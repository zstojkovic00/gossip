package rs.ac.fon.gossip.collector.http.route


class RoutePattern private constructor(val template: String, val parts: List<Part>) {

    sealed interface Part {
        fun matches(value: String): Boolean
    }

    data class Literal(val text: String) : Part {
        override fun matches(value: String) = value == text
    }

    data object Parameter : Part {
        override fun matches(value: String) = true
    }

    val literalCount: Int = parts.count { it is Literal }

    fun matches(pathParts: List<String>): Boolean =
        pathParts.size == parts.size &&
            parts.zip(pathParts).all { (part, value) -> part.matches(value) }

    companion object {
        private val PARAMETER = Regex("\\{[A-Za-z][A-Za-z0-9_]*}")

        fun parse(template: String): RoutePattern {
            require(template.startsWith("/")) { "Route pattern '$template' must start with '/'" }
            val parts = template.removePrefix("/").split("/").map { part ->
                if (PARAMETER.matches(part)) {
                    Parameter
                }
                else {
                    require(part.isNotEmpty()) { "Route pattern '$template' has an empty part" }
                    require('{' !in part && '}' !in part) {
                        "Route pattern '$template' has a malformed parameter '$part'"
                    }
                    Literal(part)
                }
            }
            return RoutePattern(template, parts)
        }
    }
}
