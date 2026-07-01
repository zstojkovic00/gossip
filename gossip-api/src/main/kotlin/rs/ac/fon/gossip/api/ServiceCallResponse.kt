package rs.ac.fon.gossip.api

data class ServiceCallResponse(
    val id: String,
    val src: String,
    val dst: String,
    val srcProcessName: String?,
    val dstProcessName: String?,
    val dstPort: Int?,
    val method: String?,
    val url: String?,
    val status: Int?,
    val timestamp: Long?,
)
