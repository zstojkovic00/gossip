package rs.ac.fon.gossip.api.flow

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rs.ac.fon.gossip.api.flow.conformance.ConformanceService
import rs.ac.fon.gossip.api.flow.conformance.ConformanceResponse
import rs.ac.fon.gossip.api.flow.conformance.toResponse

@RestController
@RequestMapping("/api/v1/flows")
class FlowController(
    private val flowService: FlowService,
    private val conformanceService: ConformanceService
) {
    @GetMapping
    fun getFlows(): ResponseEntity<List<FlowResponse>> =
        ResponseEntity.ok(
            flowService.getFlows().map { it.toResponse() }
        )

    @GetMapping("/{id}")
    fun getFlow(@PathVariable id: String): ResponseEntity<FlowResponse> =
        ResponseEntity.ok(flowService.getFlowById(id).toResponse())

    @PostMapping
    fun createFlow(@RequestBody request: FlowRequest): ResponseEntity<FlowResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(flowService.createFlow(request).toResponse())

    @PutMapping("/{id}")
    fun updateFlow(@PathVariable id: String, @RequestBody request: FlowRequest): ResponseEntity<FlowResponse> =
        ResponseEntity.ok(flowService.updateFlow(id, request).toResponse())

    @DeleteMapping("/{id}")
    fun deleteFlow(@PathVariable id: String): ResponseEntity<Unit> {
        flowService.deleteFlow(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/conformance")
    fun checkFlowConformance(@PathVariable id: String): ResponseEntity<ConformanceResponse> =
        ResponseEntity.ok(conformanceService.checkConformance(id).toResponse())
}
