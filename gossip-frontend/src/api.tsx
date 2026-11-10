export interface Service {
    ip: string
    name: string | null
}

export interface Connection {
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

export async function fetchTopology(): Promise<Topology> {
    const response = await fetch('/api/v1/topology')

    if (!response.ok) {
        throw new Error(`Error fetching topology ${response.status}`)
    }

    return response.json()
}
