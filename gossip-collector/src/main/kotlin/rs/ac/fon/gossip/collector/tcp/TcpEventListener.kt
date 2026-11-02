package rs.ac.fon.gossip.collector.tcp

import gossip.TcpEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class TcpEventListener(
    private val connectionService: ConnectionService
) {
    @KafkaListener(topics = ["\${spring.kafka.topics.tcp-event}"])
    fun onTcpEvent(msg: TcpEvent) {
        connectionService.process(msg)
    }
}
