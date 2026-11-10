package rs.ac.fon.gossip.api.topology

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/topology")
class TopologyController(
    private val topologyService: TopologyService
) {
    @GetMapping
    fun getTopology(): ResponseEntity<TopologyResponse> =
        ResponseEntity.ok(topologyService.getTopology().toResponse())
}
