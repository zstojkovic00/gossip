package rs.ac.fon.gossip.api.topology

import org.springframework.stereotype.Service

@Service
class TopologyService(
    private val topologyRepository: TopologyRepository
) {
    fun getTopology(): Topology = topologyRepository.findTopology()
}
