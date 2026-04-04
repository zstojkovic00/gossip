package rs.ac.fon.gossip.collector

import gossip.TcpEvent
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service

@Service
class TcpStateListener(private val driver: Driver) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${kafka.topic.tcp-events:tcp-events}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        concurrency = "\${spring.kafka.consumer.concurrency}"
    )
    fun onTcpStateChange(
        msg: TcpEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?
    ) {
        log.info(
            "[partition={} offset={}] pid={} comm={} {}:{} -> {}:{} state={}",
            partition, offset,
            msg.pid, msg.comm,
            msg.saddr, msg.sport,
            msg.daddr, msg.dport,
            msg.state
        )

        driver.session().use { session ->
            session.run(
                "MERGE (src:Service {name: \$src}) MERGE (dst:Service {name: \$dst}) MERGE (src)-[:CALLS]->(dst)",
                mapOf("src" to msg.comm.toString(), "dst" to msg.daddr.toString())
            )
        }
    }
}
