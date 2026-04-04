APP=tcp_listener

.PHONY: build
build: gen $(APP)

CONFIG?=kafka/kafka-config.json

.PHONY: run
run: build
	sudo ./$(APP) $(CONFIG)

.PHONY: gen
gen: vmlinux ebpf/gen_tcp_bpfel.go

.PHONY: vmlinux
vmlinux: ebpf/kernel/vmlinux.h

.PHONY: fmt
fmt:
	go fmt ./...

.PHONY: clean
clean:
	-rm $(APP)
	-rm ebpf/gen_tcp_bpfel.go ebpf/gen_tcp_bpfel.o
	-rm ebpf/kernel/vmlinux.h

$(APP): ebpf/gen_tcp_bpfel.go
	CGO_ENABLED=0 go build -o $(APP) .

ebpf/kernel/vmlinux.h:
	bpftool btf dump file /sys/kernel/btf/vmlinux format c > ebpf/kernel/vmlinux.h

ebpf/gen_tcp_bpfel.go: ebpf/kernel/tcp.bpf.c
	go generate ./ebpf/...
