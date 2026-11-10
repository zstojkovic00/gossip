package rs.ac.fon.gossip.api.operations

data class ServiceOperations(
    val ip: String,
    val port: Long,
    val operations: List<Operation>,
)

data class Operation(
    val method: String,
    val path: String,
)
