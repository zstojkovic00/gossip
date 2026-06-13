APP=gossip-agent

.PHONY: build
build: gen $(APP)

CONFIG?=kafka/kafka-config.json

.PHONY: run
run: build
	sudo ./$(APP) $(CONFIG)

.PHONY: gen
gen: vmlinux ebpf/tcp/gen_tcp_bpfel.go ebpf/http/gen_http_bpfel.go

.PHONY: vmlinux
vmlinux: ebpf/vmlinux.h

.PHONY: fmt
fmt:
	go fmt ./...

.PHONY: clean
clean:
	-rm $(APP)
	-rm ebpf/tcp/gen_tcp_bpfel.go ebpf/tcp/gen_tcp_bpfel.o
	-rm ebpf/http/gen_http_bpfel.go ebpf/http/gen_http_bpfel.o
	-rm ebpf/vmlinux.h

$(APP): ebpf/tcp/gen_tcp_bpfel.go ebpf/http/gen_http_bpfel.go kafka/schemas/tcp_event.avsc $(wildcard kafka/*.go) $(wildcard ebpf/tcp/*.go) $(wildcard ebpf/http/*.go) *.go
	CGO_ENABLED=0 go build -o $(APP) .

ebpf/vmlinux.h:
	bpftool btf dump file /sys/kernel/btf/vmlinux format c > ebpf/vmlinux.h

ebpf/tcp/gen_tcp_bpfel.go: ebpf/tcp/kernel/tcp.bpf.c
	go generate ./ebpf/tcp/...

ebpf/http/gen_http_bpfel.go: ebpf/http/kernel/http.bpf.c
	go generate ./ebpf/http/...
