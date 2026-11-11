package rs.ac.fon.gossip.api.topology

import org.springframework.stereotype.Service

@Service
class TopologyService(
    private val topologyRepository: TopologyRepository
) {
    fun getTopology(): Topology = topologyRepository.findTopology()

    fun getInteractions(): List<Interaction> = topologyRepository.findInteractions()

    fun getInteractionsByKeys(interactionKeys: List<String>): List<Interaction> = topologyRepository.findInteractionsByKeys(interactionKeys)

    fun getConnectionsByKeys(connectionKeys: List<String>): List<Connection> = topologyRepository.findConnectionsByKeys(connectionKeys)
}
