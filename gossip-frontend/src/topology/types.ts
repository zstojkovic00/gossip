export interface Service {
    ip: string
    name: string | null
}

export interface Connection {
    key: string
    src: string
    dst: string
    dport: number
    count: number
    firstSeen: number
    lastSeen: number
}

export interface Interaction {
    key: string
    src: string
    dst: string
    dport: number
    protocol: string
    method: string
    path: string
    count: number
    errorCount: number
    firstSeen: number
    lastSeen: number
}

export interface Topology {
    services: Service[]
    connections: Connection[]
    interactions: Interaction[]
}

export interface Operation {
    method: string
    path: string
}

export interface ServiceOperations {
    ip: string
    port: number
    operations: Operation[]
}
