package rs.ac.fon.gossip.collector.http

data class HttpInteraction(
    val key: String,
    val src: String,
    val dst: String,
    val dport: Int,
    val protocol: String,
    val method: String,
    val path: String,
    val status: Int,
    val timestamp: Long,
)
