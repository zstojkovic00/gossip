import {BaseEdge, EdgeLabelRenderer, getBezierPath, type Edge, type EdgeProps} from '@xyflow/react'
import {Group, Paper, Text, UnstyledButton} from '@mantine/core'
import type {ConformanceStatus, Step} from './types.ts'
import {getStepOperation} from './status.ts'
import StatusDot from './StatusDot.tsx'

export type StepEdgeData = {
    // all steps from the same caller to the same service share one edge
    steps: Step[]
    statuses: Record<string, ConformanceStatus>
    selectedStepId: string | null
    onSelectStep?: (id: string) => void
}

export type StepEdgeType = Edge<StepEdgeData, 'step'>

// The calls between two services on the flow graph, one row per step
const StepEdge = (props: EdgeProps<StepEdgeType>) => {
    const {id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, markerEnd, style, data} = props
    const [path, labelX, labelY] = getBezierPath({sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition})
    const {steps, statuses, selectedStepId, onSelectStep} = data!

    return (
        <>
            <BaseEdge id={id} path={path} markerEnd={markerEnd} style={style}/>
            <EdgeLabelRenderer>
                <Paper
                    withBorder
                    shadow="xs"
                    px={8}
                    py={4}
                    className="nodrag nopan"
                    style={{
                        position: 'absolute',
                        pointerEvents: 'all',
                        transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
                    }}
                >
                    {steps.map(step => (
                            <UnstyledButton key={step.id} display="block" onClick={() => onSelectStep?.(step.id)}>
                                <Group gap={6} wrap="nowrap">
                                    <StatusDot status={statuses[step.id]} size={8}/>
                                    <Text
                                        ff="monospace"
                                        fz={12}
                                        fw={step.id === selectedStepId ? 700 : 400}
                                        td={step.id === selectedStepId ? 'underline' : undefined}
                                    >
                                        {getStepOperation(step)}
                                    </Text>
                                </Group>
                            </UnstyledButton>
                    ))}
                </Paper>
            </EdgeLabelRenderer>
        </>
    )
}

export default StepEdge
