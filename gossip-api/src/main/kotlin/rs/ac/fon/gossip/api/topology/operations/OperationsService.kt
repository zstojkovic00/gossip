package rs.ac.fon.gossip.api.topology.operations

import org.springframework.stereotype.Service
import rs.ac.fon.gossip.api.topology.TopologyService

@Service
class OperationsService(
    private val topologyService: TopologyService
) {
    fun getOperations(): List<ServiceOperations> {
        val interactions = topologyService.getInteractions()

        return interactions
            .groupBy { it.dst to it.dport }
            .map { (owner, interactions) ->
                ServiceOperations(
                    ip = owner.first,
                    port = owner.second,
                    operations = interactions
                        .map { Operation(it.method, it.path) }
                        .distinct()
                        .sortedWith(compareBy({ it.path }, { it.method })),
                )
            }
            .sortedWith(compareBy({ it.ip }, { it.port }))
    }
}
