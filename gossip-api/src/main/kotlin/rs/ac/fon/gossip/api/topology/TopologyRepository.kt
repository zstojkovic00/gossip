package rs.ac.fon.gossip.api.topology

import org.neo4j.driver.Driver
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
                src: src.ip, dst: dst.ip, dport: c.dport,
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
            connections = record["connections"].asList { c ->
                Connection(
                    src = c["src"].asString(),
                    dst = c["dst"].asString(),
                    dport = c["dport"].asLong(),
                    count = c["count"].asLong(),
                    firstSeen = c["firstSeen"].asLong(),
                    lastSeen = c["lastSeen"].asLong(),
                )
            },
            interactions = record["interactions"].asList { i ->
                Interaction(
                    key = i["key"].asString(),
                    src = i["src"].asString(),
                    dst = i["dst"].asString(),
                    dport = i["dport"].asLong(),
                    protocol = i["protocol"].asString(),
                    method = i["method"].asString(),
                    path = i["path"].asString(),
                    count = i["count"].asLong(),
                    errorCount = i["errorCount"].asLong(),
                    firstSeen = i["firstSeen"].asLong(),
                    lastSeen = i["lastSeen"].asLong(),
                )
            },
        )
    }
}
