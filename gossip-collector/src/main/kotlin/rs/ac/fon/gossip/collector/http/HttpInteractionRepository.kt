package rs.ac.fon.gossip.collector.http

import org.neo4j.driver.Driver
import org.neo4j.driver.Values
import org.springframework.stereotype.Repository

@Repository
class HttpInteractionRepository(
    private val neo4jDriver: Driver
) {
    fun merge(interaction: HttpInteraction) {
        val query = """
            MERGE (src:Service {ip: ${'$'}src})
            MERGE (dst:Service {ip: ${'$'}dst})
            MERGE (i:Interaction {key: ${'$'}key})
            ON CREATE SET i.dport = ${'$'}dport, i.protocol = ${'$'}protocol, i.method = ${'$'}method,
                          i.path = ${'$'}path, i.status = ${'$'}status,
                          i.count = 0, i.firstSeen = ${'$'}ts, i.lastSeen = ${'$'}ts
            SET i.count = i.count + 1,
                i.firstSeen = CASE WHEN ${'$'}ts < i.firstSeen THEN ${'$'}ts ELSE i.firstSeen END,
                i.lastSeen  = CASE WHEN ${'$'}ts > i.lastSeen  THEN ${'$'}ts ELSE i.lastSeen  END
            MERGE (src)-[:SENDS]->(i)
            MERGE (i)-[:TO]->(dst)
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "src", interaction.src,
                        "dst", interaction.dst,
                        "key", interaction.key,
                        "dport", interaction.dport,
                        "protocol", interaction.protocol,
                        "method", interaction.method,
                        "path", interaction.path,
                        "status", interaction.status,
                        "ts", interaction.timestamp,
                    )
                ).consume()
            }
        }
    }
}
