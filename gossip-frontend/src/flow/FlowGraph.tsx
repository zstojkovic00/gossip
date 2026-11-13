import {Background, Controls, MarkerType, ReactFlow} from '@xyflow/react'
import type {ConformanceStatus, Step} from './types.ts'
import {CONFORMANCE_SEVERITY, getStatusCssColor} from './status.ts'
import ServiceNode, {SERVICE_HEIGHT, SERVICE_WIDTH, type ServiceNodeType} from '../topology/ServiceNode.tsx'
import StepEdge, {type StepEdgeType} from './StepEdge.tsx'
import {layoutTopToBottom} from '../topology/graphLayout.ts'

const nodeTypes = {service: ServiceNode}
const edgeTypes = {step: StepEdge}

interface Props {
    steps: Step[]
    statuses?: Record<string, ConformanceStatus>
    selectedStepId?: string | null
    onSelectStep?: (id: string) => void
    // the editor highlights the current caller, and shows it even before it has any step
    selectedService?: string | null
    onSelectService?: (ip: string) => void
}

// Services are nodes like in the topology, steps are the calls between them
const FlowGraph = ({steps, statuses = {}, selectedStepId = null, onSelectStep, selectedService = null, onSelectService}: Props) => {
    const stepsByCall = new Map<string, Step[]>()
    steps.forEach(step => {
        const call = `${step.caller}->${step.ip}`
        stepsByCall.set(call, [...(stepsByCall.get(call) ?? []), step])
    })

    const edges: StepEdgeType[] = [...stepsByCall.entries()].map(([call, callSteps]) => {
        const status = CONFORMANCE_SEVERITY.find(severity => callSteps.some(step => statuses[step.id] === severity))
        const color = getStatusCssColor(status)
        const selected = callSteps.some(step => step.id === selectedStepId)
        return {
            id: call,
            source: callSteps[0].caller,
            target: callSteps[0].ip,
            type: 'step',
            markerEnd: {type: MarkerType.ArrowClosed, color},
            style: {stroke: color, strokeWidth: selected ? 3 : 2, strokeDasharray: status === 'MISSING' ? '6 4' : undefined},
            data: {steps: callSteps, statuses, selectedStepId, onSelectStep},
        }
    })

    const ips = [...new Set([...steps.flatMap(step => [step.caller, step.ip]), ...(selectedService ? [selectedService] : [])])]
    // extra space between rows leaves room for the operation labels on the edges
    const positions = layoutTopToBottom(ips, edges, SERVICE_WIDTH, SERVICE_HEIGHT, 120)

    const nodes: ServiceNodeType[] = ips.map(ip => ({
        id: ip,
        type: 'service',
        position: positions.get(ip)!,
        selected: ip === selectedService,
        data: {
            ip,
            ports: [...new Set(steps.filter(step => step.ip === ip).map(step => step.port))].sort((a, b) => a - b),
        },
    }))

    return (
        <ReactFlow
            // remount when steps change so the view fits the new graph
            key={steps.map(step => `${step.id}:${step.caller}:${step.ip}`).join('|') + (steps.length === 0 ? selectedService : '')}
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            onEdgeClick={(_, edge) => onSelectStep?.(edge.data!.steps[0].id)}
            onNodeClick={(_, node) => onSelectService?.(node.id)}
            nodesConnectable={false}
            fitView
            fitViewOptions={{maxZoom: 1}}
        >
            <Background/>
            <Controls showInteractive={false}/>
        </ReactFlow>
    )
}

export default FlowGraph
