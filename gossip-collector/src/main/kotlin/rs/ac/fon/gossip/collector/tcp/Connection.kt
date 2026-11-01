package rs.ac.fon.gossip.collector.tcp

data class Connection(
    val src: String,
    val dst: String,
    val dport: Int,
    val comm: String,
    val timestamp: Long,
)
