package rs.ac.fon.gossip.api.flow.conformance

import rs.ac.fon.gossip.api.flow.Protocol

data class ConformanceResponse(
    val flowId: String,
    val name: String,
    val status: ConformanceStatus,
    val steps: List<StepConformanceResponse>,
)

data class StepConformanceResponse(
    val id: String,
    val protocol: Protocol,
    val caller: String,
    val ip: String,
    val port: Long,
    val method: String?,
    val path: String?,
    val after: List<String>,
    val status: ConformanceStatus,
    val count: Long,
    val errorCount: Long,
    val lastSeen: Long?,
)

fun Conformance.toResponse() = ConformanceResponse(flowId, name, status, steps.map { it.toResponse() })

fun StepConformance.toResponse() = StepConformanceResponse(
    step.id, step.protocol, step.caller, step.ip, step.port, step.method, step.path, step.after,
    status, count, errorCount, lastSeen,
)
