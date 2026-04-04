package kafka

type TcpEvent struct {
	Pid   int64  `avro:"pid"`
	Saddr string `avro:"saddr"`
	Daddr string `avro:"daddr"`
	Sport int32  `avro:"sport"`
	Dport int32  `avro:"dport"`
	State string `avro:"state"`
	Comm  string `avro:"comm"`
}
