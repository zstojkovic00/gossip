package rs.ac.fon.gossip.collector.tcp

data class TcpConnection(
    val src: String,
    val dst: String,
    val dport: Int,
    val comm: String,
    val timestamp: Long,
)
