package rs.ac.fon.gossip.collector.http

import gossip.HttpEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class HttpInteractionService(
    private val httpInteractionRepository: HttpInteractionRepository,
    @Value("\${gossip.allowed-subnet-prefix}") private val subnetPrefix: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun record(msg: HttpEvent) {
        if (!msg.saddr.startsWith(subnetPrefix) || !msg.daddr.startsWith(subnetPrefix)) return

        log.info("[HTTP] ${msg.saddr} → ${msg.daddr}:${msg.dport} ${msg.method} ${msg.url} → ${msg.status}")

        val path = msg.url.substringBefore('?')
        val key = "${msg.saddr}|${msg.daddr}|${msg.dport}|${msg.method}|$path|${msg.status}"

        httpInteractionRepository.merge(
            HttpInteraction(
                key = key,
                src = msg.saddr,
                dst = msg.daddr,
                dport = msg.dport,
                protocol = "http",
                method = msg.method,
                path = path,
                status = msg.status,
                timestamp = msg.timestamp,
            )
        )
    }
}
