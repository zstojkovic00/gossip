package graphdb

type Topology struct {
	Services     []Service     `json:"services"`
	Connections  []Connection  `json:"connections"`
	Interactions []Interaction `json:"interactions"`
}

type Service struct {
	IP   string  `json:"ip"`
	Name *string `json:"name"`
}

type Connection struct {
	Src       string `json:"src"`
	Dst       string `json:"dst"`
	Dport     int64  `json:"dport"`
	Count     int64  `json:"count"`
	FirstSeen int64  `json:"firstSeen"`
	LastSeen  int64  `json:"lastSeen"`
}

type Interaction struct {
	Key       string `json:"key"`
	Src       string `json:"src"`
	Dst       string `json:"dst"`
	Dport     int64  `json:"dport"`
	Protocol  string `json:"protocol"`
	Method    string `json:"method"`
	Path      string `json:"path"`
	Status    int64  `json:"status"`
	Count     int64  `json:"count"`
	FirstSeen int64  `json:"firstSeen"`
	LastSeen  int64  `json:"lastSeen"`
}

const topologyQuery = `
CALL {
  MATCH (s:Service)
  RETURN collect({ip: s.ip, name: s.name}) AS services
}
CALL {
  MATCH (src:Service)-[c:CONNECTS_TO]->(dst:Service)
  RETURN collect({
    src: src.ip, dst: dst.ip, dport: c.dport,
    count: c.count, firstSeen: c.firstSeen, lastSeen: c.lastSeen
  }) AS connections
}
CALL {
  MATCH (src:Service)-[:SENDS]->(i:Interaction)-[:TO]->(dst:Service)
  RETURN collect({
    key: i.key, src: src.ip, dst: dst.ip, dport: i.dport,
    protocol: i.protocol, method: i.method, path: i.path, status: i.status,
    count: i.count, firstSeen: i.firstSeen, lastSeen: i.lastSeen
  }) AS interactions
}
RETURN services, connections, interactions`

func toService(m map[string]any) Service {
	return Service{
		IP:   value[string](m, "ip"),
		Name: optional[string](m, "name"),
	}
}

func toConnection(m map[string]any) Connection {
	return Connection{
		Src:       value[string](m, "src"),
		Dst:       value[string](m, "dst"),
		Dport:     value[int64](m, "dport"),
		Count:     value[int64](m, "count"),
		FirstSeen: value[int64](m, "firstSeen"),
		LastSeen:  value[int64](m, "lastSeen"),
	}
}

func toInteraction(m map[string]any) Interaction {
	return Interaction{
		Key:       value[string](m, "key"),
		Src:       value[string](m, "src"),
		Dst:       value[string](m, "dst"),
		Dport:     value[int64](m, "dport"),
		Protocol:  value[string](m, "protocol"),
		Method:    value[string](m, "method"),
		Path:      value[string](m, "path"),
		Status:    value[int64](m, "status"),
		Count:     value[int64](m, "count"),
		FirstSeen: value[int64](m, "firstSeen"),
		LastSeen:  value[int64](m, "lastSeen"),
	}
}
