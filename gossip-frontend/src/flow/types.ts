export type Protocol = 'HTTP' | 'TCP'

export interface Step {
    id: string
    protocol: Protocol
    caller: string
    ip: string
    port: number
    method: string | null
    path: string | null
    after: string[]
}

export interface Flow {
    id: string
    name: string
    steps: Step[]
}

export interface FlowRequest {
    name: string
    steps: Step[]
}

export type ConformanceStatus = 'OBSERVED' | 'FAILING' | 'MISSING'

export interface StepConformance extends Step {
    status: ConformanceStatus
    count: number
    errorCount: number
    lastSeen: number | null
}

export interface Conformance {
    flowId: string
    name: string
    status: ConformanceStatus
    steps: StepConformance[]
}

// What the flow list shares with the page next to it, through the router outlet
export interface FlowsContext {
    flows: Flow[]
    reload: () => Promise<void>
}
