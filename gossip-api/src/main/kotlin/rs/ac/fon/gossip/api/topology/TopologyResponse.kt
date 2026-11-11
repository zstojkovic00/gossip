package rs.ac.fon.gossip.api.topology

data class TopologyResponse(
    val services: List<ServiceResponse>,
    val connections: List<ConnectionResponse>,
    val interactions: List<InteractionResponse>,
)

data class ServiceResponse(val ip: String, val name: String?)

data class ConnectionResponse(
    val key: String,
    val src: String,
    val dst: String,
    val dport: Long,
    val count: Long,
    val firstSeen: Long,
    val lastSeen: Long,
)

data class InteractionResponse(
    val key: String,
    val src: String,
    val dst: String,
    val dport: Long,
    val protocol: String,
    val method: String,
    val path: String,
    val count: Long,
    val errorCount: Long,
    val firstSeen: Long,
    val lastSeen: Long,
)

fun Topology.toResponse() = TopologyResponse(
    services = services.map { it.toResponse() },
    connections = connections.map { it.toResponse() },
    interactions = interactions.map { it.toResponse() },
)

fun Service.toResponse() = ServiceResponse(ip, name)

fun Connection.toResponse() = ConnectionResponse(key, src, dst, dport, count, firstSeen, lastSeen)

fun Interaction.toResponse() = InteractionResponse(
    key, src, dst, dport, protocol, method, path, count, errorCount, firstSeen, lastSeen,
)
