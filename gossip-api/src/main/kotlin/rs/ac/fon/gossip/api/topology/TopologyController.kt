package rs.ac.fon.gossip.api.topology

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/topology")
class TopologyController(
    private val topologyService: TopologyService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getTopology(): ResponseEntity<TopologyResponse> {
        val topology = topologyService.getTopology()
        log.info("Found {} services, {} connections, {} interactions",
            topology.services.size, topology.connections.size, topology.interactions.size
        )
        return ResponseEntity.ok(TopologyResponse.from(topology))
    }
}
