package rs.ac.fon.gossip.api.operations

import org.springframework.stereotype.Service

@Service
class OperationsService(
    private val operationRepository: OperationRepository
) {
    fun getOperations(): List<ServiceOperations> = operationRepository.findAll()
}
