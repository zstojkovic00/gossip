package rs.ac.fon.gossip.api.flow.conformance

import rs.ac.fon.gossip.api.flow.Step

data class Conformance(
    val flowId: String,
    val name: String,
    val status: ConformanceStatus,
    val steps: List<StepConformance>,
)

data class StepConformance(
    val step: Step,
    val status: ConformanceStatus,
    val count: Long,
    val errorCount: Long,
    val lastSeen: Long?,
)

enum class ConformanceStatus { OBSERVED, FAILING, MISSING }
