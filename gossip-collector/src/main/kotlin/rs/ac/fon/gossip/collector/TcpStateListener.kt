package rs.ac.fon.gossip.collector

import gossip.TcpEvent
import org.neo4j.driver.Values
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

@Service
class TcpStateListener(
    private val neo4jDriver: org.neo4j.driver.Driver,
    @Value("\${gossip.allowed-subnet-prefix}") private val subnetPrefix: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${spring.kafka.topics.tcp-event}"])
    fun onTcpStateChange(
        msg: TcpEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?
    ) {
        if (msg.oldstate != "SYN_SENT") return
        if (!msg.saddr.startsWith(subnetPrefix) || !msg.daddr.startsWith(subnetPrefix)) return

        log.info("[TCP] ${msg.comm} ${msg.saddr}:${msg.sport} → ${msg.daddr}:${msg.dport}")

        val query = """
            MERGE (src:Service {ip: ${'$'}srcIp})
              ON CREATE SET src.comm = ${'$'}srcComm
            MERGE (dst:Service {ip: ${'$'}dstIp})
            MERGE (src)-[:CALLS {dstPort: ${'$'}dstPort}]->(dst)
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "srcIp", msg.saddr,
                        "srcComm", msg.comm,
                        "dstIp", msg.daddr,
                        "dstPort", msg.dport,
                    )
                ).consume()
            }
        }
    }
}
