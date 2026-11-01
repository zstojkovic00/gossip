package rs.ac.fon.gossip.collector.http

import gossip.HttpEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class HttpEventListener(
    private val httpInteractionService: HttpInteractionService
) {
    @KafkaListener(topics = ["\${spring.kafka.topics.http-event}"])
    fun onHttpEvent(msg: HttpEvent) {
        httpInteractionService.record(msg)
    }
}
