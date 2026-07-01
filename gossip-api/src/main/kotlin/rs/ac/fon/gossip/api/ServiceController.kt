package rs.ac.fon.gossip.api

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/services")
class ServiceController(
    private val neo4jDriver: org.neo4j.driver.Driver
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/calls")
    fun getServiceCalls(): ResponseEntity<List<ServiceCallResponse>> {
        val query = """
            MATCH (src:Service)-[r:CALLS]->(dst:Service)
            RETURN src, r, dst
        """.trimIndent()
        val result = neo4jDriver.executableQuery(query).execute()

        val calls = result.records().map { record ->
            val src = record["src"].asNode()
            val r = record["r"].asRelationship()
            val dst = record["dst"].asNode()

            ServiceCallResponse(
                id = r.elementId(),
                src = src.get("ip").asString(),
                dst = dst.get("ip").asString(),
                srcProcessName = src.get("comm").asString(null),
                dstProcessName = dst.get("comm").asString(null),
                dstPort = r.get("dstPort").let { v -> if (v.isNull) null else v.asInt() },
                method = r.get("method").asString(null),
                url = r.get("url").asString(null),
                status = r.get("status").let { v -> if (v.isNull) null else v.asInt() },
                timestamp = r.get("timestamp").let { v -> if (v.isNull) null else v.asLong() },
            )
        }

        log.info("Found {} calls", calls.size)
        return ResponseEntity.ok(calls)
    }
}
