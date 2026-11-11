package rs.ac.fon.gossip.api.flow

data class Flow(
    val id: String,
    val name: String,
    val steps: List<Step>,
)

data class Step(
    val id: String,
    val caller: String,
    val ip: String,
    val port: Long,
    val method: String?,
    val path: String?,
    val after: List<String>,
)
