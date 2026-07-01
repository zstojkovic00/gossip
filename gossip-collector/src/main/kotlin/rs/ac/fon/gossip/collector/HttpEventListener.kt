package rs.ac.fon.gossip.collector

import gossip.HttpEvent
import org.neo4j.driver.Values
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

@Service
class HttpEventListener(
    private val neo4jDriver: org.neo4j.driver.Driver,
    @Value("\${gossip.allowed-subnet-prefix}") private val subnetPrefix: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${spring.kafka.topics.http-event}"])
    fun onHttpEvent(
        msg: HttpEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?
    ) {
        if (!msg.saddr.startsWith(subnetPrefix) || !msg.daddr.startsWith(subnetPrefix)) return

        log.info("[HTTP] ${msg.saddr} → ${msg.daddr}:${msg.dport} ${msg.method} ${msg.url} → ${msg.status}")

        val query = """
            MERGE (src:Service {ip: ${'$'}saddr})
            MERGE (dst:Service {ip: ${'$'}daddr})
            CREATE (src)-[:CALLS {dstPort: ${'$'}dport, method: ${'$'}method, url: ${'$'}url, status: ${'$'}status, timestamp: ${'$'}timestamp}]->(dst)
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "saddr", msg.saddr,
                        "daddr", msg.daddr,
                        "dport", msg.dport,
                        "method", msg.method,
                        "url", msg.url,
                        "status", msg.status,
                        "timestamp", msg.timestamp,
                    )
                ).consume()
            }
        }
    }
}
