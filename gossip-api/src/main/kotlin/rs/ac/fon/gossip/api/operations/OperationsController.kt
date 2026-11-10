package rs.ac.fon.gossip.api.operations

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/operations")
class OperationsController(
    private val operationsService: OperationsService
) {
    @GetMapping
    fun getOperations(): ResponseEntity<OperationsResponse> =
        ResponseEntity.ok(operationsService.getOperations().toResponse())
}
