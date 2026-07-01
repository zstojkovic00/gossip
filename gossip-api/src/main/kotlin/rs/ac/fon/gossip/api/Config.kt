package rs.ac.fon.gossip.api

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {

    @Bean
    fun neo4jDriver(
        @Value("\${neo4j.uri}") uri: String,
        @Value("\${neo4j.username}") username: String,
        @Value("\${neo4j.password}") password: String,
    ): Driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))
}
