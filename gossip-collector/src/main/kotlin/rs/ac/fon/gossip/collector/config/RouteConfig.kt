package rs.ac.fon.gossip.collector.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import rs.ac.fon.gossip.collector.http.route.FunctionalityResolver
import rs.ac.fon.gossip.collector.http.route.RoutePattern

@Configuration
class RouteConfig {

    @Bean
    fun functionalityResolver(@Value("\${gossip.http.route-patterns}") file: Resource): FunctionalityResolver {
        val lines = file.inputStream.bufferedReader().use { it.readLines() }

        val patterns = lines.mapIndexedNotNull {
            index, line ->
            val text  = line.trim()
            if(text.isEmpty() || text.startsWith("#")) null
            else try {
                RoutePattern.parse(text)
            }  catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("${file.description}, line ${index + 1}: ${e.message}", e)
            }
        }

        return FunctionalityResolver(patterns = patterns)
    }
}
