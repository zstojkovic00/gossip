package rs.ac.fon.gossip.collector.config

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class Neo4jConfig {

    @Bean
    fun neo4jDriver(
        @Value("\${spring.neo4j.uri}") neo4jUri: String,
        @Value("\${spring.neo4j.authentication.username}") username: String,
        @Value("\${spring.neo4j.authentication.password}") password: String
    ): Driver =
        GraphDatabase.driver(neo4jUri, AuthTokens.basic(username, password))
}