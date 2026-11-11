package rs.ac.fon.gossip.api.flow

data class FlowResponse(val id: String, val name: String, val steps: List<StepResponse>)

data class StepResponse(
    val id: String,
    val caller: String,
    val ip: String,
    val port: Long,
    val method: String?,
    val path: String?,
    val after: List<String>,
)

fun Flow.toResponse() = FlowResponse(id, name, steps.map { it.toResponse() })

fun Step.toResponse() = StepResponse(id, caller, ip, port, method, path, after)
