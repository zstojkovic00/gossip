package rs.ac.fon.gossip.api.flow

data class FlowRequest(val name: String, val steps: List<StepRequest>)

data class StepRequest(
    val id: String,
    val protocol: Protocol,
    val caller: String,
    val ip: String,
    val port: Long,
    val method: String? = null,
    val path: String? = null,
    val after: List<String> = emptyList(),
)

fun FlowRequest.toFlow(id: String) = Flow(id, name, steps.map { it.toStep() })

fun StepRequest.toStep() = Step(id, protocol, caller, ip, port, method, path, after)
