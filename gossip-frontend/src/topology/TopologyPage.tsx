import {useEffect, useState} from 'react'
import {Background, Controls, ReactFlow, type Edge} from '@xyflow/react'
import {Center, Loader} from '@mantine/core'
import {getTopology} from './topologyService.ts'
import type {Topology} from './types.ts'
import {toTopologyGraph, type EdgeData} from './topologyGraph.ts'
import ServiceNode from './ServiceNode.tsx'
import BottomPanel from './BottomPanel.tsx'

const nodeTypes = {service: ServiceNode}

const TopologyPage = () => {
    const [topology, setTopology] = useState<Topology | null>(null)
    const [selectedEdge, setSelectedEdge] = useState<Edge | null>(null)
    const [selectedService, setSelectedService] = useState<string | null>(null)

    useEffect(() => {
        getTopology()
            .then(response => setTopology(response))
            .catch(err => console.error(err))
    }, [])

    const selectEdge = (edge: Edge) => {
        setSelectedEdge(edge)
        setSelectedService(null)
    }

    const selectService = (ip: string) => {
        setSelectedService(ip)
        setSelectedEdge(null)
    }

    const clearSelection = () => {
        setSelectedService(null)
        setSelectedEdge(null)
    }

    // fitView only runs when React Flow mounts, so it has to mount with the nodes already there
    if (!topology) {
        return <Center h="100vh"><Loader/></Center>
    }

    const graph = toTopologyGraph(topology)

    // Clicking a service highlights who calls it and whom it calls, everything else fades out
    function isHighlighted(edge: Edge): boolean {
        if (edge.id === selectedEdge?.id) return true
        return selectedService !== null && (edge.source === selectedService || edge.target === selectedService)
    }

    const neighbours = new Set(graph.edges.filter(isHighlighted).flatMap(edge => [edge.source, edge.target]))
    const somethingSelected = selectedService !== null || selectedEdge !== null

    const nodes = graph.nodes.map(node => ({
        ...node,
        style: {opacity: !somethingSelected || neighbours.has(node.id) ? 1 : 0.3},
    }))

    const edges = graph.edges.map(edge => {
        if (isHighlighted(edge)) {
            return {...edge, animated: true, zIndex: 1, style: {...edge.style, stroke: 'var(--mantine-color-blue-6)', strokeWidth: 3}}
        }
        return somethingSelected ? {...edge, style: {...edge.style, opacity: 0.15}} : edge
    })

    return (
        <div style={{position: 'relative', height: '100vh'}}>
            <ReactFlow
                nodes={nodes}
                nodeTypes={nodeTypes}
                edges={edges}
                onEdgeClick={(_, edge) => selectEdge(edge)}
                onNodeClick={(_, node) => selectService(node.id)}
                onPaneClick={clearSelection}
                nodesConnectable={false}
                fitView
            >
                <Background/>
                <Controls showInteractive={false}/>
            </ReactFlow>
            {selectedEdge && (
                <BottomPanel
                    key={selectedEdge.id}
                    src={selectedEdge.source}
                    dst={selectedEdge.target}
                    data={selectedEdge.data as unknown as EdgeData}
                    onClose={() => setSelectedEdge(null)}
                />
            )}
        </div>
    )
}

export default TopologyPage
