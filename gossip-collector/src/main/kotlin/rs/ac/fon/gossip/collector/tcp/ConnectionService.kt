package rs.ac.fon.gossip.collector.tcp

import gossip.TcpEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ConnectionService(
    private val connectionRepository: ConnectionRepository,
    @Value("\${gossip.allowed-subnet-prefix}") private val subnetPrefix: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun record(msg: TcpEvent) {
        if (!msg.saddr.startsWith(subnetPrefix) || !msg.daddr.startsWith(subnetPrefix)) return

        log.info("[TCP] ${msg.comm} ${msg.saddr}:${msg.sport} → ${msg.daddr}:${msg.dport}")

        connectionRepository.merge(
            Connection(
                src = msg.saddr,
                dst = msg.daddr,
                dport = msg.dport,
                comm = msg.comm,
                timestamp = msg.timestamp,
            )
        )
    }
}
