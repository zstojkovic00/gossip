import '@xyflow/react/dist/style.css'
import './App.css'
import {useEffect, useState} from 'react'
import {fetchTopology, type Topology} from './api.tsx'
import {ReactFlow, type Edge} from '@xyflow/react'
import {toGraph, type EdgeData} from './graph.tsx'
import BottomPanel from './components/BottomPanel.tsx'

export default function App() {
    const [topology, setTopology] = useState<Topology>({services: [], connections: [], interactions: []})
    const [selectedEdge, setSelectedEdge] = useState<Edge | null>(null)

    useEffect(() => {
        fetchTopology()
            .then(data => setTopology(data))
            .catch(err => console.error(err))
    }, [])

    const {nodes, edges} = toGraph(topology)

    return (
        <div className="app">
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onEdgeClick={(_, edge) => setSelectedEdge(edge)}
                fitView
            />
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
