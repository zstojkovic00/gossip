package rs.ac.fon.gossip.collector

import gossip.TcpEvent
import org.neo4j.driver.Values
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

@Service
class TcpStateListener(
    private val neo4jDriver: org.neo4j.driver.Driver
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${spring.kafka.topics.tcp-event}"])
    fun onTcpStateChange(
        msg: TcpEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?
    ) {
        log.info(""" partition: $partition, offset: $offset, key: $key msg: $msg """)

        val src = "${msg.saddr}:${msg.sport}"
        val dst = "${msg.daddr}:${msg.dport}"
        val query = """
            MERGE (src:SocketAddress {id: ${'$'}srcId})
              ON CREATE SET src.address = ${'$'}srcAddr, src.port = ${'$'}srcPort, src.comm = ${'$'}srcComm

            MERGE (dst:SocketAddress {id: ${'$'}dstId})
              ON CREATE SET dst.address = ${'$'}dstAddr, dst.port = ${'$'}dstPort

            MERGE (src)-[:CONNECTED_TO]->(dst)
        """.trimIndent()

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                tx.run(
                    query, Values.parameters(
                        "srcId", src,
                        "srcAddr", msg.saddr,
                        "srcPort", msg.sport,
                        "srcComm", msg.comm,
                        "dstId", dst,
                        "dstAddr", msg.daddr,
                        "dstPort", msg.dport,
                    )
                ).consume()
            }
        }
    }
}
