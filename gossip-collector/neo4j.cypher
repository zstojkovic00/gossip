// Delete all nodes and relationships
MATCH (n) DETACH DELETE n

// Find all CONNECTED_TO relationships where dst.port is 8085
MATCH (src:SocketAddress)-[r:CONNECTED_TO]->(dst:SocketAddress)
  WHERE dst.port = 8085
  RETURN src, r, dst