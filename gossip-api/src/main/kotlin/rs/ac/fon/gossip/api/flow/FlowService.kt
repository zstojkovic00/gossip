package rs.ac.fon.gossip.api.flow

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class FlowService(
    private val flowRepository: FlowRepository
) {
    fun getFlows(): List<Flow> = flowRepository.findAll()

    fun getFlowById(id: String): Flow =
        flowRepository.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Flow '$id' not found")

    fun createFlow(request: FlowRequest): Flow {
        val flow = request.toFlow(UUID.randomUUID().toString())
        validate(flow)
        flowRepository.save(flow)
        return flow
    }

    fun updateFlow(id: String, request: FlowRequest): Flow {
        flowRepository.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Flow '$id' not found")
        val flow = request.toFlow(id)
        validate(flow)
        flowRepository.update(flow)
        return flow
    }

    fun deleteFlow(id: String) {
        flowRepository.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Flow '$id' not found")
        flowRepository.delete(id)
    }

}

internal fun validate(flow: Flow) {
    require(flow.name.isNotBlank()) { "Flow name must not be blank" }
    require(flow.steps.isNotEmpty()) { "Flow must have at least one step" }

    val ids = flow.steps.map { it.id }
    val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Duplicate step ids: $duplicates" }

    flow.steps.forEach { step ->
        val a = step.method
        val b = step.path
        require((a == null) == (b == null)) { "Step '${step.id}' must have both method and path, or neither" }
        step.after.forEach { require(it in ids) { "Step '${step.id}' comes after unknown step '$it'" } }
    }

    // TODO: has cycle
}
