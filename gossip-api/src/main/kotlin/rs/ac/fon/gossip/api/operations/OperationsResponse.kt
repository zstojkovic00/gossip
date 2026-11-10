package rs.ac.fon.gossip.api.operations

data class OperationsResponse(val services: List<ServiceOperationsResponse>)

data class ServiceOperationsResponse(val ip: String, val port: Long, val operations: List<OperationResponse>)

data class OperationResponse(val method: String, val path: String)

fun List<ServiceOperations>.toResponse() = OperationsResponse(map { it.toResponse() })

fun ServiceOperations.toResponse() = ServiceOperationsResponse(ip, port, operations.map { it.toResponse() })

fun Operation.toResponse() = OperationResponse(method, path)
