package rs.ac.fon.gossip.collector

import gossip.HttpEvent
import org.neo4j.driver.Values
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

@Service
class HttpEventListener(
    private val neo4jDriver: org.neo4j.driver.Driver
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${spring.kafka.topics.http-event}"])
    fun onHttpEvent(
        msg: HttpEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        val endpoint = "${msg.method} ${msg.url}"
        log.info("[HTTP] ${msg.saddr}:${msg.sport} → ${msg.daddr}:${msg.dport} $endpoint → ${msg.status}")

        val query = """
            MATCH (src:SocketAddress)-[r:CONNECTED_TO]->(dst:SocketAddress)
            WHERE src.address = ${'$'}saddr
              AND dst.address = ${'$'}daddr
              AND dst.port = ${'$'}dport
              AND NOT ${'$'}endpoint IN coalesce(r.endpoints, [])
            SET r.endpoints = coalesce(r.endpoints, []) + ${'$'}endpoint
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "saddr", msg.saddr,
                        "daddr", msg.daddr,
                        "dport", msg.dport,
                        "endpoint", endpoint,
                    )
                ).consume()
            }
        }
    }
}
