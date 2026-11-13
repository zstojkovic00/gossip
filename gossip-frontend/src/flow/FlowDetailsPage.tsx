import {useEffect, useState} from 'react'
import {useNavigate, useOutletContext, useParams} from 'react-router'
import {Alert, Badge, Box, Button, Group, ScrollArea, Table, Text, Title} from '@mantine/core'
import {checkConformance, deleteFlow, errorMessage, getFlow} from './flowService.ts'
import type {Conformance, Flow, FlowsContext} from './types.ts'
import {getStatusColor, getStepOperation, getStepRoute} from './status.ts'
import FlowGraph from './FlowGraph.tsx'

// One flow drawn with the status of every step, and the numbers behind each status
const FlowDetailsPage = () => {
    const {flowId} = useParams()
    const {reload} = useOutletContext<FlowsContext>()
    const navigate = useNavigate()

    const [flow, setFlow] = useState<Flow | null>(null)
    const [conformance, setConformance] = useState<Conformance | null>(null)
    const [selectedStepId, setSelectedStepId] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        Promise.all([getFlow(flowId!), checkConformance(flowId!)])
            .then(([flowResponse, conformanceResponse]) => {
                setFlow(flowResponse)
                setConformance(conformanceResponse)
                setError(null)
            })
            .catch(err => setError(errorMessage(err)))
    }, [flowId])

    const editFlow = () => {
        navigate(`/flows/${flowId}/edit`)
    }

    const removeFlow = async () => {
        if (!flow || !window.confirm(`Delete flow "${flow.name}"?`)) return
        await deleteFlow(flow.id)
        await reload()
        navigate('/flows')
    }

    if (!flow) {
        return error ? <Alert color="red" m="md">{error}</Alert> : null
    }

    const statuses = Object.fromEntries((conformance?.steps ?? []).map(step => [step.id, step.status]))

    return (
        <Box style={{display: 'flex', flexDirection: 'column', height: '100vh'}}>
            <Group justify="space-between" p="md" style={{borderBottom: '1px solid var(--mantine-color-gray-3)'}}>
                <Group gap="sm">
                    <Title order={3}>{flow.name}</Title>
                    {conformance && <Badge color={getStatusColor(conformance.status)}>{conformance.status}</Badge>}
                </Group>
                <Group gap="xs">
                    <Button variant="default" onClick={editFlow}>Edit</Button>
                    <Button variant="light" color="red" onClick={removeFlow}>Delete</Button>
                </Group>
            </Group>

            {error && <Alert color="red" m="md">{error}</Alert>}

            <Box style={{flex: 1, minHeight: 300}}>
                <FlowGraph
                    steps={flow.steps}
                    statuses={statuses}
                    selectedStepId={selectedStepId}
                    onSelectStep={setSelectedStepId}
                />
            </Box>

            <ScrollArea h="35%" style={{borderTop: '1px solid var(--mantine-color-gray-3)'}}>
                <Table highlightOnHover>
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Step</Table.Th>
                            <Table.Th>Operation</Table.Th>
                            <Table.Th>Route</Table.Th>
                            <Table.Th>Status</Table.Th>
                            <Table.Th>Calls</Table.Th>
                            <Table.Th>Errors</Table.Th>
                            <Table.Th>Last seen</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {(conformance?.steps ?? []).map(step => (
                            <Table.Tr
                                key={step.id}
                                bg={step.id === selectedStepId ? 'var(--mantine-color-blue-light)' : undefined}
                                style={{cursor: 'pointer'}}
                                onClick={() => setSelectedStepId(step.id)}
                            >
                                <Table.Td><Badge size="sm" variant="light" color="gray">{step.id}</Badge></Table.Td>
                                <Table.Td><Text size="sm" ff="monospace">{getStepOperation(step)}</Text></Table.Td>
                                <Table.Td><Text size="sm" ff="monospace">{getStepRoute(step)}</Text></Table.Td>
                                <Table.Td><Badge size="sm" color={getStatusColor(step.status)}>{step.status}</Badge></Table.Td>
                                <Table.Td>{step.count}</Table.Td>
                                <Table.Td c={step.errorCount > 0 ? 'red' : undefined}>{step.errorCount}</Table.Td>
                                <Table.Td>{step.lastSeen ? new Date(step.lastSeen).toLocaleString() : '—'}</Table.Td>
                            </Table.Tr>
                        ))}
                    </Table.Tbody>
                </Table>
            </ScrollArea>
        </Box>
    )
}

export default FlowDetailsPage
