import {type Node, type Edge} from '@xyflow/react'
import {type ServiceRelation} from './api.tsx'

export interface HttpCall {
    method: string
    url: string
    status: number
    timestamp: number | null
    payload: string | null
}

export interface PortSection {
    dstPort: number | null
    calls: HttpCall[]
}

export interface EdgeData {
    srcProcessName: string | null
    dstProcessName: string | null
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
export function toGraph(serviceRelations: ServiceRelation[]): { nodes: Node[], edges: Edge[] } {
    const services = new Set<string>()
    serviceRelations.forEach(sr => {
        services.add(sr.src)
        services.add(sr.dst)
    })

    const nodes: Node[] = [...services].map(ip => ({
        id: ip,
        position: {x: 0, y: 0},
        data: {label: ip},
    }))

    // group by (src, dst) pair, one visual edge per pair, all ports inside
    const edgeMap = new Map<string, ServiceRelation[]>()
    serviceRelations.forEach(sr => {
        const key = `${sr.src}->${sr.dst}`
        if (!edgeMap.has(key)) edgeMap.set(key, [])
        edgeMap.get(key)!.push(sr)
    })

    const edges: Edge[] = [...edgeMap.entries()].map(([key, relations]) => {
        const first = relations[0]

        // group by dstPort, one section per port in the sidebar
        const portMap = new Map<number | null, HttpCall[]>()
        relations.forEach(r => {
            if (!portMap.has(r.dstPort)) portMap.set(r.dstPort, [])
            if (r.method !== null && r.url !== null && r.status !== null) {
                portMap.get(r.dstPort)!.push({method: r.method, url: r.url, status: r.status, timestamp: r.timestamp, payload: null})
            }
        })

        return {
            id: key,
            source: first.src,
            target: first.dst,
            data: {
                srcProcessName: first.srcProcessName,
                dstProcessName: first.dstProcessName,
                ports: [...portMap.entries()].map(([dstPort, calls]) => ({dstPort, calls})),
            } satisfies EdgeData,
        }
    })

    return {nodes: applyTopologicalOrder(nodes, edges), edges}
}
