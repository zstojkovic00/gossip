package rs.ac.fon.gossip.collector.http

import gossip.HttpEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import rs.ac.fon.gossip.collector.http.route.FunctionalityResolver

@Service
class HttpInteractionService(
    private val httpInteractionRepository: HttpInteractionRepository,
    private val functionalityResolver: FunctionalityResolver,
    @Value("\${gossip.allowed-subnet-prefix}") private val subnetPrefix: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun process(msg: HttpEvent) {
        if (!msg.saddr.startsWith(subnetPrefix) || !msg.daddr.startsWith(subnetPrefix)) return

        log.info("[HTTP] ${msg.saddr} → ${msg.daddr}:${msg.dport} ${msg.method} ${msg.url} → ${msg.status}")

        val path = functionalityResolver.resolve(msg.url.substringBefore('?'))
        val key = "${msg.saddr}|${msg.daddr}|${msg.dport}|${msg.method}|$path"

        httpInteractionRepository.merge(
            HttpInteraction(
                key = key,
                src = msg.saddr,
                dst = msg.daddr,
                dport = msg.dport,
                protocol = "http",
                method = msg.method,
                path = path,
                failed = msg.status >= 400,
                timestamp = msg.timestamp,
            )
        )
    }
}
