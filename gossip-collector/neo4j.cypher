// Obrisi sve nodove i relacije
MATCH (n) DETACH DELETE n

// Nadji sve CONNECTED_TO gde je dst.port jednak 8085
MATCH (src:SocketAddress)-[r:CONNECTED_TO]->(dst:SocketAddress)
  WHERE dst.port = 8085
  RETURN src, r, dst