package rs.ac.fon.gossip.api.operations

import org.neo4j.driver.Driver
import org.springframework.stereotype.Repository

@Repository
class OperationRepository(
    private val neo4jDriver: Driver
) {
    fun findAll(): List<ServiceOperations> {
        val query = """
            MATCH (i:Interaction)-[:TO]->(dst:Service)
            WITH DISTINCT dst.ip AS ip, i.dport AS port, i.method AS method, i.path AS path
            ORDER BY ip, port, path, method
            RETURN ip, port, collect({method: method, path: path}) AS operations
            ORDER BY ip, port
        """.trimIndent()

        return neo4jDriver.executableQuery(query).execute().records().map { r ->
            ServiceOperations(
                ip = r["ip"].asString(),
                port = r["port"].asLong(),
                operations = r["operations"].asList { o ->
                    Operation(
                        method = o["method"].asString(),
                        path = o["path"].asString(),
                    )
                },
            )
        }
    }
}
