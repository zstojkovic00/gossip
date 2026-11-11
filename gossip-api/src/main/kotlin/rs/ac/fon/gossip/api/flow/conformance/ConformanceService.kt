package rs.ac.fon.gossip.api.flow.conformance

import org.springframework.stereotype.Service
import rs.ac.fon.gossip.api.flow.FlowService
import rs.ac.fon.gossip.api.flow.Protocol
import rs.ac.fon.gossip.api.flow.Step
import rs.ac.fon.gossip.api.topology.Connection
import rs.ac.fon.gossip.api.topology.Interaction
import rs.ac.fon.gossip.api.topology.TopologyService

@Service
class ConformanceService(
    private val topologyService: TopologyService,
    private val flowService: FlowService,
) {
    fun checkConformance(id: String): Conformance {
        val flow = flowService.getFlowById(id)

        val httpSteps = flow.steps.filter { it.protocol == Protocol.HTTP }
        val tcpSteps = flow.steps.filter { it.protocol == Protocol.TCP }

        val interactions = topologyService.getInteractionsByKeys(httpSteps.map { it.key() })
        val connections = topologyService.getConnectionsByKeys(tcpSteps.map { it.key() })

        val steps = flow.steps.map { step ->
            when (step.protocol) {
                Protocol.HTTP -> checkHttpStep(step, interactions.find { it.key == step.key() })
                Protocol.TCP -> checkTcpStep(step, connections.find { it.key == step.key() })
            }
        }

        return Conformance(flow.id, flow.name, steps.maxOf { it.status }, steps)
    }

    private fun checkHttpStep(step: Step, interaction: Interaction?): StepConformance = when {
        interaction == null ->
            StepConformance(step, ConformanceStatus.MISSING, 0, 0, null)
        interaction.errorCount > interaction.successCount() ->
            StepConformance(step, ConformanceStatus.FAILING, interaction.count, interaction.errorCount, interaction.lastSeen)
        else ->
            StepConformance(step, ConformanceStatus.OBSERVED, interaction.count, interaction.errorCount, interaction.lastSeen)
    }

    private fun Interaction.successCount() = count - errorCount

    private fun checkTcpStep(step: Step, connection: Connection?): StepConformance = when {
        connection == null ->
            StepConformance(step, ConformanceStatus.MISSING, 0, 0, null)
        else ->
            StepConformance(step, ConformanceStatus.OBSERVED, connection.count, 0, connection.lastSeen)
    }

    private fun Step.key(): String = when (protocol) {
        Protocol.HTTP -> "$caller|$ip|$port|$method|$path"
        Protocol.TCP -> "$caller|$ip|$port"
    }
}
