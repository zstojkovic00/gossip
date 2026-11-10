package rs.ac.fon.gossip.api.topology

data class Topology(
    val services: List<Service>,
    val connections: List<Connection>,
    val interactions: List<Interaction>,
)

data class Service(
    val ip: String,
    val name: String?,
)

data class Connection(
    val src: String,
    val dst: String,
    val dport: Long,
    val count: Long,
    val firstSeen: Long,
    val lastSeen: Long,
)

data class Interaction(
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
