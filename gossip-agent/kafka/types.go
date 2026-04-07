package kafka

type TcpEvent struct {
	Skaddr   string `avro:"skaddr"`
	Pid      int32  `avro:"pid"`
	Saddr    string `avro:"saddr"`
	Daddr    string `avro:"daddr"`
	Sport    int32  `avro:"sport"`
	Dport    int32  `avro:"dport"`
	NewState string `avro:"newstate"`
	OldState string `avro:"oldstate"`
	Comm     string `avro:"comm"`
}
