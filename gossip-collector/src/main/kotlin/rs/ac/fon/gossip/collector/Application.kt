package rs.ac.fon.gossip.collector

import jakarta.annotation.PostConstruct
import org.neo4j.driver.Driver
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application(
    private val neo4jDriver: Driver
) {
    @PostConstruct
    fun createConstraints() {
        neo4jDriver.executableQuery(
            "CREATE CONSTRAINT service_ip IF NOT EXISTS FOR (s:Service) REQUIRE s.ip IS UNIQUE"
        ).execute()
        neo4jDriver.executableQuery(
            "CREATE CONSTRAINT interaction_key IF NOT EXISTS FOR (i:Interaction) REQUIRE i.key IS UNIQUE"
        ).execute()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
