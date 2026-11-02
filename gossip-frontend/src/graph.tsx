import {type Node, type Edge} from '@xyflow/react'
import {type Connection, type Interaction, type Topology} from './api.tsx'

export interface PortSection {
    dport: number
    connection: Connection | null
    interactions: Interaction[]
}

export interface EdgeData {
    ports: PortSection[]
}

// Arranges nodes in layers based on how many services call them.
// Services that nobody calls go on top, their dependencies below them, and so on.
function applyTopologicalOrder(nodes: Node[], edges: Edge[]): Node[] {
    // count how many incoming edges each node has
    const nodeDegree = new Map<string, number>()
    nodes.forEach(node => nodeDegree.set(node.id, 0))
    edges.forEach(edge => nodeDegree.set(edge.target, (nodeDegree.get(edge.target) ?? 0) + 1))

    const layers: Node[][] = []
    const visited = new Set<string>()
    // entry points, nobody calls these, so they belong in the first layer
    let currentLayer = nodes.filter(node => nodeDegree.get(node.id) === 0)

    while (currentLayer.length > 0) {
        layers.push(currentLayer)
        currentLayer.forEach(node => visited.add(node.id))

        // find everyone the current layer calls that we haven't placed yet
        const nextLayer = new Set<string>()
        currentLayer.forEach(node => {
            edges
                .filter(edge => edge.source === node.id && !visited.has(edge.target))
                .forEach(edge => nextLayer.add(edge.target))
        })
        currentLayer = nodes.filter(node => nextLayer.has(node.id))
    }

    // nodes stuck in a cycle never get visited
    const unvisited = nodes.filter(node => !visited.has(node.id))
    if (unvisited.length > 0) layers.push(unvisited)

    const NODE_WIDTH = 200
    const NODE_HEIGHT = 150

    // assign positions, each layer is a row, nodes within a layer are centered horizontally
    return layers.flatMap((layer, layerIndex) =>
        layer.map((node, nodeIndex) => ({
            ...node,
            position: {
                x: nodeIndex * NODE_WIDTH - (layer.length * NODE_WIDTH) / 2 + NODE_WIDTH / 2,
                y: layerIndex * NODE_HEIGHT,
            },
        }))
    )
}
export function toGraph(topology: Topology): { nodes: Node[], edges: Edge[] } {
    const nodes: Node[] = topology.services.map(service => ({
        id: service.ip,
        position: {x: 0, y: 0},
        data: {label: service.name ?? service.ip},
    }))

    // one visual edge per (src, dst) pair, one section per port inside it
    const edgeMap = new Map<string, Map<number, PortSection>>()

    function portSection(src: string, dst: string, dport: number): PortSection {
        const key = `${src}->${dst}`
        if (!edgeMap.has(key)) edgeMap.set(key, new Map())
        const ports = edgeMap.get(key)!
        if (!ports.has(dport)) ports.set(dport, {dport, connection: null, interactions: []})
        return ports.get(dport)!
    }

    topology.connections.forEach(c => {
        portSection(c.src, c.dst, c.dport).connection = c
    })
    topology.interactions.forEach(i => {
        portSection(i.src, i.dst, i.dport).interactions.push(i)
    })

    const edges: Edge[] = [...edgeMap.entries()].map(([key, ports]) => {
        const [source, target] = key.split('->')
        return {
            id: key,
            source,
            target,
            data: {
                ports: [...ports.values()],
            } satisfies EdgeData,
        }
    })

    return {nodes: applyTopologicalOrder(nodes, edges), edges}
}
