import {MarkerType, type Edge} from '@xyflow/react'
import type {Connection, Interaction, Topology} from './types.ts'
import {layoutTopToBottom} from './graphLayout.ts'
import {SERVICE_HEIGHT, SERVICE_WIDTH, type ServiceNodeType} from './ServiceNode.tsx'

export interface PortSection {
    dport: number
    connection: Connection | null
    interactions: Interaction[]
}

export interface EdgeData {
    ports: PortSection[]
}

export function toTopologyGraph(topology: Topology): { nodes: ServiceNodeType[], edges: Edge[] } {
    // one visual edge per (src, dst) pair, one section per port inside it
    const edgeMap = new Map<string, Map<number, PortSection>>()

    function portSection(src: string, dst: string, dport: number): PortSection {
        const key = `${src}->${dst}`
        if (!edgeMap.has(key)) edgeMap.set(key, new Map())
        const ports = edgeMap.get(key)!
        if (!ports.has(dport)) ports.set(dport, {dport, connection: null, interactions: []})
        return ports.get(dport)!
    }

    // A service talking to itself (e.g. Kafka's broker to its own controller) is not communication between services
    const connections = topology.connections.filter(c => c.src !== c.dst)
    const interactions = topology.interactions.filter(i => i.src !== i.dst)

    connections.forEach(c => {
        portSection(c.src, c.dst, c.dport).connection = c
    })
    interactions.forEach(i => {
        portSection(i.src, i.dst, i.dport).interactions.push(i)
    })

    const edges: Edge[] = [...edgeMap.entries()].map(([key, ports]) => {
        const [source, target] = key.split('->')
        const sections = [...ports.values()]
        const color = 'var(--mantine-color-gray-5)'
        return {
            id: key,
            source,
            target,
            // curved edges spread out where many callers meet one service, and a wide hit area makes them easy to click
            interactionWidth: 30,
            markerEnd: {type: MarkerType.ArrowClosed, color},
            style: {stroke: color, strokeWidth: 2},
            data: {
                ports: sections,
            } satisfies EdgeData,
        }
    })

    const positions = layoutTopToBottom(
        topology.services.map(service => service.ip),
        edges,
        SERVICE_WIDTH,
        SERVICE_HEIGHT,
    )

    const nodes: ServiceNodeType[] = topology.services.map(service => ({
        id: service.ip,
        type: 'service',
        position: positions.get(service.ip)!,
        data: {
            ip: service.ip,
            ports: [...new Set(connections.filter(c => c.dst === service.ip).map(c => c.dport))].sort((a, b) => a - b),
        },
    }))

    return {nodes, edges}
}
