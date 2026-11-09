package rs.ac.fon.gossip.collector.tcp

import org.neo4j.driver.Driver
import org.neo4j.driver.Values
import org.springframework.stereotype.Repository

@Repository
class TcpConnectionRepository(
    private val neo4jDriver: Driver
) {
    fun merge(tcpConnection: TcpConnection) {
        val query = """
            MERGE (src:Service {ip: ${'$'}src})
              ON CREATE SET src.comm = ${'$'}comm
            MERGE (dst:Service {ip: ${'$'}dst})
            MERGE (src)-[c:CONNECTS_TO {dport: ${'$'}dport}]->(dst)
              ON CREATE SET c.count = 0, c.firstSeen = ${'$'}ts, c.lastSeen = ${'$'}ts
            SET c.count = c.count + 1,
                c.firstSeen = CASE WHEN ${'$'}ts < c.firstSeen THEN ${'$'}ts ELSE c.firstSeen END,
                c.lastSeen  = CASE WHEN ${'$'}ts > c.lastSeen  THEN ${'$'}ts ELSE c.lastSeen  END
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "src", tcpConnection.src,
                        "comm", tcpConnection.comm,
                        "dst", tcpConnection.dst,
                        "dport", tcpConnection.dport,
                        "ts", tcpConnection.timestamp,
                    )
                ).consume()
            }
        }
    }
}
