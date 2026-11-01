(
  until cypher-shell -u neo4j -p password "RETURN 1" > /dev/null 2>&1; do sleep 1; done
  cypher-shell -u neo4j -p password -f /init/constraints.cypher
) &
