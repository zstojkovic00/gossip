package rs.ac.fon.gossip.api.topology

data class TopologyResponse(
    val services: List<ServiceResponse>,
    val connections: List<ConnectionResponse>,
    val interactions: List<InteractionResponse>,
) {
    companion object {
        fun from(topology: Topology) = TopologyResponse(
            services = topology.services.map { ServiceResponse.from(it) },
            connections = topology.connections.map { ConnectionResponse.from(it) },
            interactions = topology.interactions.map { InteractionResponse.from(it) },
        )
    }
}

data class ServiceResponse(
    val ip: String,
    val name: String?,
) {
    companion object {
        fun from(service: Service) = ServiceResponse(
            ip = service.ip,
            name = service.name,
        )
    }
}

data class ConnectionResponse(
    val src: String,
    val dst: String,
    val dport: Long,
    val count: Long,
    val firstSeen: Long,
    val lastSeen: Long,
) {
    companion object {
        fun from(connection: Connection) = ConnectionResponse(
            src = connection.src,
            dst = connection.dst,
            dport = connection.dport,
            count = connection.count,
            firstSeen = connection.firstSeen,
            lastSeen = connection.lastSeen,
        )
    }
}

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
) {
    companion object {
        fun from(interaction: Interaction) = InteractionResponse(
            key = interaction.key,
            src = interaction.src,
            dst = interaction.dst,
            dport = interaction.dport,
            protocol = interaction.protocol,
            method = interaction.method,
            path = interaction.path,
            count = interaction.count,
            errorCount = interaction.errorCount,
            firstSeen = interaction.firstSeen,
            lastSeen = interaction.lastSeen,
        )
    }
}
