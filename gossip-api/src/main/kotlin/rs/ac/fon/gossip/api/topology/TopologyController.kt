package rs.ac.fon.gossip.api.topology

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rs.ac.fon.gossip.api.topology.operations.OperationsResponse
import rs.ac.fon.gossip.api.topology.operations.OperationsService
import rs.ac.fon.gossip.api.topology.operations.toResponse

@RestController
@RequestMapping("/api/v1/topology")
class TopologyController(
    private val topologyService: TopologyService,
    private val operationsService: OperationsService,
) {
    @GetMapping
    fun getTopology(): ResponseEntity<TopologyResponse> =
        ResponseEntity.ok(topologyService.getTopology().toResponse())

    @GetMapping("/operations")
    fun getOperations(): ResponseEntity<OperationsResponse> =
        ResponseEntity.ok(operationsService.getOperations().toResponse())
}
