package rs.ac.fon.gossip.api.topology

import org.neo4j.driver.Driver
import org.neo4j.driver.Value
import org.springframework.stereotype.Repository

@Repository
class TopologyRepository(
    private val neo4jDriver: Driver
) {
    fun findTopology(): Topology {
        val query = """
            CALL {
              MATCH (s:Service)
              RETURN collect({ip: s.ip, name: s.name}) AS services
            }
            CALL {
              MATCH (src:Service)-[c:CONNECTS_TO]->(dst:Service)
              RETURN collect({
                key: src.ip + '|' + dst.ip + '|' + toString(c.dport), src: src.ip, dst: dst.ip, dport: c.dport,
                count: c.count, firstSeen: c.firstSeen, lastSeen: c.lastSeen
              }) AS connections
            }
            CALL {
              MATCH (src:Service)-[:SENDS]->(i:Interaction)-[:TO]->(dst:Service)
              RETURN collect({
                key: i.key, src: src.ip, dst: dst.ip, dport: i.dport,
                protocol: i.protocol, method: i.method, path: i.path,
                count: i.count, errorCount: i.errorCount, firstSeen: i.firstSeen, lastSeen: i.lastSeen
              }) AS interactions
            }
            RETURN services, connections, interactions
        """.trimIndent()
        val record = neo4jDriver.executableQuery(query).execute().records().single()

        return Topology(
            services = record["services"].asList { s ->
                Service(
                    ip = s["ip"].asString(),
                    name = s["name"].asString(null),
                )
            },
            connections = record["connections"].asList { it.toConnection() },
            interactions = record["interactions"].asList { it.toInteraction() },
        )
    }

    fun findInteractions(): List<Interaction> {
        val query = """
            MATCH (src:Service)-[:SENDS]->(i:Interaction)-[:TO]->(dst:Service)
            RETURN {
              key: i.key, src: src.ip, dst: dst.ip, dport: i.dport,
              protocol: i.protocol, method: i.method, path: i.path,
              count: i.count, errorCount: i.errorCount, firstSeen: i.firstSeen, lastSeen: i.lastSeen
            } AS interaction
        """.trimIndent()

        return neo4jDriver.executableQuery(query).execute().records().map { it["interaction"].toInteraction() }
    }

    fun findInteractionsByKeys(keys: List<String>): List<Interaction> {
        val query = """
            MATCH (src:Service)-[:SENDS]->(i:Interaction)-[:TO]->(dst:Service)
            WHERE i.key IN ${'$'}keys
            RETURN {
              key: i.key, src: src.ip, dst: dst.ip, dport: i.dport,
              protocol: i.protocol, method: i.method, path: i.path,
              count: i.count, errorCount: i.errorCount, firstSeen: i.firstSeen, lastSeen: i.lastSeen
            } AS interaction
        """.trimIndent()

        return neo4jDriver.executableQuery(query)
            .withParameters(mapOf("keys" to keys))
            .execute().records().map { it["interaction"].toInteraction() }
    }

    fun findConnectionsByKeys(keys: List<String>): List<Connection> {
        val query = """
            MATCH (src:Service)-[c:CONNECTS_TO]->(dst:Service)
            WITH src, c, dst, src.ip + '|' + dst.ip + '|' + toString(c.dport) AS key
            WHERE key IN ${'$'}keys
            RETURN {
              key: key, src: src.ip, dst: dst.ip, dport: c.dport,
              count: c.count, firstSeen: c.firstSeen, lastSeen: c.lastSeen
            } AS connection
        """.trimIndent()

        return neo4jDriver.executableQuery(query)
            .withParameters(mapOf("keys" to keys))
            .execute().records().map { it["connection"].toConnection() }
    }

    private fun Value.toConnection() = Connection(
        key = this["key"].asString(),
        src = this["src"].asString(),
        dst = this["dst"].asString(),
        dport = this["dport"].asLong(),
        count = this["count"].asLong(),
        firstSeen = this["firstSeen"].asLong(),
        lastSeen = this["lastSeen"].asLong(),
    )

    private fun Value.toInteraction() = Interaction(
        key = this["key"].asString(),
        src = this["src"].asString(),
        dst = this["dst"].asString(),
        dport = this["dport"].asLong(),
        protocol = this["protocol"].asString(),
        method = this["method"].asString(),
        path = this["path"].asString(),
        count = this["count"].asLong(),
        errorCount = this["errorCount"].asLong(),
        firstSeen = this["firstSeen"].asLong(),
        lastSeen = this["lastSeen"].asLong(),
    )
}
