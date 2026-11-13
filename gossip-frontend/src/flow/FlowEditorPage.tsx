import {useEffect, useState} from 'react'
import {useNavigate, useOutletContext, useParams} from 'react-router'
import {
    ActionIcon, Alert, Badge, Box, Button, Group, NavLink, ScrollArea, Select, Stack, Switch, Table, Text, TextInput, Title,
} from '@mantine/core'
import {IconPlus, IconSearch, IconX} from '@tabler/icons-react'
import {createFlow, errorMessage, getFlow, updateFlow} from './flowService.ts'
import {getOperations, getTopology} from '../topology/topologyService.ts'
import type {FlowsContext, Step} from './types.ts'
import type {Operation} from '../topology/types.ts'
import {getStepOperation, getStepRoute} from './status.ts'
import {buildCatalog, filterCatalog, type CatalogEntry} from './catalog.ts'
import {serviceName} from '../topology/serviceNames.ts'
import FlowGraph from './FlowGraph.tsx'

const BORDER = '1px solid var(--mantine-color-gray-3)'

const nextStepId = (steps: Step[]) => {
    const numbers = steps.map(step => Number(step.id.replace('s', ''))).filter(n => !isNaN(n))
    return `s${Math.max(0, ...numbers) + 1}`
}

// Builds a flow by picking operations from the catalog; /flows/new creates one, /flows/:flowId/edit replaces one
const FlowEditorPage = () => {
    const {flowId} = useParams()
    const {reload} = useOutletContext<FlowsContext>()
    const navigate = useNavigate()

    const [name, setName] = useState('')
    const [steps, setSteps] = useState<Step[]>([])
    const [caller, setCaller] = useState<string | null>(null)
    const [parallel, setParallel] = useState(false)
    const [search, setSearch] = useState('')
    const [opened, setOpened] = useState<Set<string>>(new Set())
    const [catalog, setCatalog] = useState<CatalogEntry[]>([])
    const [services, setServices] = useState<string[]>([])
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        Promise.all([getOperations(), getTopology()])
            .then(([operations, topology]) => {
                setCatalog(buildCatalog(operations, topology))
                setServices(topology.services.map(service => service.ip).sort())
            })
            .catch(err => setError(errorMessage(err)))
    }, [])

    // When editing, continue from the service that was called last
    useEffect(() => {
        if (!flowId) return
        getFlow(flowId)
            .then(flow => {
                setName(flow.name)
                setSteps(flow.steps)
                setCaller(flow.steps[flow.steps.length - 1]?.ip ?? null)
            })
            .catch(err => setError(errorMessage(err)))
    }, [flowId])

    const previous = steps[steps.length - 1]
    // In parallel mode the new step is a sibling of the previous one: same caller, same predecessors
    const effectiveCaller = parallel && previous ? previous.caller : caller

    const addStep = (entry: CatalogEntry, operation: Operation | null) => {
        if (!effectiveCaller) {
            setError('Choose who starts the flow first')
            return
        }
        if (entry.ip === effectiveCaller) return
        const step: Step = {
            id: nextStepId(steps),
            protocol: operation ? 'HTTP' : 'TCP',
            caller: effectiveCaller,
            ip: entry.ip,
            port: entry.port,
            method: operation?.method ?? null,
            path: operation?.path ?? null,
            after: !previous ? [] : parallel ? previous.after : [previous.id],
        }
        setSteps([...steps, step])
        setError(null)
        // "next, next": the service that was just called makes the next call
        if (!parallel) setCaller(entry.ip)
    }

    // Steps that came after the removed one now come after whatever it came after, so the chain stays connected
    const removeStep = (removed: Step) => {
        setSteps(steps
            .filter(step => step.id !== removed.id)
            .map(step => step.after.includes(removed.id)
                ? {...step, after: [...new Set([...step.after.filter(id => id !== removed.id), ...removed.after])]}
                : step))
    }

    const toggleGroup = (key: string) => {
        const next = new Set(opened)
        if (next.has(key)) next.delete(key)
        else next.add(key)
        setOpened(next)
    }

    const save = async () => {
        try {
            const request = {name, steps}
            const saved = flowId ? await updateFlow(flowId, request) : await createFlow(request)
            await reload()
            navigate(`/flows/${saved.id}`)
        } catch (err) {
            setError(errorMessage(err))
        }
    }

    const cancel = () => {
        navigate(flowId ? `/flows/${flowId}` : '/flows')
    }

    return (
        <Box style={{display: 'flex', flexDirection: 'column', height: '100vh'}}>
            <Group justify="space-between" p="md" style={{borderBottom: BORDER}}>
                <Group gap="sm" style={{flex: 1}}>
                    <Title order={3}>{flowId ? 'Edit flow' : 'New flow'}</Title>
                    <TextInput
                        placeholder="Flow name"
                        value={name}
                        onChange={event => setName(event.currentTarget.value)}
                        style={{flex: 1, maxWidth: 400}}
                    />
                </Group>
                <Group gap="xs">
                    <Button variant="default" onClick={cancel}>Cancel</Button>
                    <Button onClick={save}>Save</Button>
                </Group>
            </Group>

            {error && <Alert color="red" mx="md" mt="md" withCloseButton onClose={() => setError(null)}>{error}</Alert>}

            <Box style={{display: 'flex', flex: 1, minHeight: 0}}>
                <Stack w={340} p="md" gap="sm" style={{borderRight: BORDER}}>
                    <Select
                        label="Caller"
                        description="Or click a service on the canvas"
                        placeholder="Who starts the flow"
                        searchable
                        data={services.map(ip => ({value: ip, label: serviceName(ip) ? `${serviceName(ip)} (${ip})` : ip}))}
                        value={effectiveCaller}
                        onChange={setCaller}
                        disabled={parallel && !!previous}
                        styles={{input: {fontFamily: 'monospace'}}}
                    />
                    <Switch
                        label="Parallel with the previous step"
                        checked={parallel}
                        disabled={!previous}
                        onChange={event => setParallel(event.currentTarget.checked)}
                    />
                    <TextInput
                        leftSection={<IconSearch size={14}/>}
                        placeholder="Search service or operation"
                        value={search}
                        onChange={event => setSearch(event.currentTarget.value)}
                    />

                    <ScrollArea style={{flex: 1}}>
                        {filterCatalog(catalog, search).map(entry => {
                            const key = `${entry.ip}:${entry.port}`
                            // a service does not call itself, so the caller's own operations cannot be added
                            const isCaller = entry.ip === effectiveCaller
                            return (
                                <NavLink
                                    key={key}
                                    label={<Text fz={13} fw={600}>{serviceName(entry.ip) ?? key}</Text>}
                                    description={
                                        <Text component="span" ff="monospace" fz={12} c="dimmed">
                                            {key} · {isCaller ? 'current caller' : entry.operations ? `${entry.operations.length} operations` : 'TCP'}
                                        </Text>
                                    }
                                    disabled={isCaller}
                                    opened={!isCaller && (search.trim() !== '' || opened.has(key))}
                                    onChange={() => toggleGroup(key)}
                                    childrenOffset={12}
                                >
                                    {entry.operations === null
                                        ? <CatalogRow label="TCP" disabled={isCaller} onAdd={() => addStep(entry, null)}/>
                                        : entry.operations.map(operation => (
                                            <CatalogRow
                                                key={`${operation.method} ${operation.path}`}
                                                label={`${operation.method} ${operation.path}`}
                                                disabled={isCaller}
                                                onAdd={() => addStep(entry, operation)}
                                            />
                                        ))}
                                </NavLink>
                            )
                        })}
                    </ScrollArea>
                </Stack>

                <Box style={{flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column'}}>
                    <Box style={{flex: 1, minHeight: 0}}>
                        <FlowGraph
                            steps={steps}
                            selectedService={effectiveCaller}
                            onSelectService={ip => {
                                setCaller(ip)
                                setParallel(false)
                            }}
                        />
                    </Box>

                    <ScrollArea h="30%" style={{borderTop: BORDER}}>
                        <Table>
                            <Table.Thead>
                                <Table.Tr>
                                    <Table.Th>Step</Table.Th>
                                    <Table.Th>Operation</Table.Th>
                                    <Table.Th>Route</Table.Th>
                                    <Table.Th>After</Table.Th>
                                    <Table.Th/>
                                </Table.Tr>
                            </Table.Thead>
                            <Table.Tbody>
                                {steps.map(step => (
                                    <Table.Tr key={step.id}>
                                        <Table.Td><Badge size="sm" variant="light" color="gray">{step.id}</Badge></Table.Td>
                                        <Table.Td><Text size="sm" ff="monospace">{getStepOperation(step)}</Text></Table.Td>
                                        <Table.Td><Text size="sm" ff="monospace">{getStepRoute(step)}</Text></Table.Td>
                                        <Table.Td><Text size="sm" c="dimmed">{step.after.join(', ') || 'first'}</Text></Table.Td>
                                        <Table.Td>
                                            <ActionIcon variant="subtle" color="gray" onClick={() => removeStep(step)}>
                                                <IconX size={16}/>
                                            </ActionIcon>
                                        </Table.Td>
                                    </Table.Tr>
                                ))}
                            </Table.Tbody>
                        </Table>
                        {steps.length === 0 && (
                            <Text c="dimmed" size="sm" p="md">
                                Choose who starts the flow, then add operations from the catalog on the left.
                            </Text>
                        )}
                    </ScrollArea>
                </Box>
            </Box>
        </Box>
    )
}

const CatalogRow = ({label, disabled, onAdd}: { label: string, disabled: boolean, onAdd: () => void }) => {
    return (
        <Group justify="space-between" wrap="nowrap" py={4} pr={4}>
            <Text ff="monospace" fz={13} truncate c={disabled ? 'dimmed' : undefined}>{label}</Text>
            <ActionIcon size="sm" variant="light" disabled={disabled} onClick={onAdd}>
                <IconPlus size={14}/>
            </ActionIcon>
        </Group>
    )
}

export default FlowEditorPage
