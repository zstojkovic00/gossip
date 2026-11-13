import {useEffect, useState} from 'react'
import {Link, Navigate, Outlet, useNavigate, useOutletContext, useParams} from 'react-router'
import {Box, Button, Center, NavLink, ScrollArea, Text} from '@mantine/core'
import {checkConformance, getFlows} from './flowService.ts'
import type {ConformanceStatus, Flow, FlowsContext} from './types.ts'
import StatusDot from './StatusDot.tsx'

// Flow list on the left, the selected flow (details or editor) on the right
const FlowsPage = () => {
    const [flows, setFlows] = useState<Flow[]>([])
    const [statuses, setStatuses] = useState<Record<string, ConformanceStatus>>({})
    const {flowId} = useParams()
    const navigate = useNavigate()

    const showFlows = async (response: Flow[]) => {
        setFlows(response)
        const conformances = await Promise.all(response.map(flow => checkConformance(flow.id)))
        setStatuses(Object.fromEntries(conformances.map(c => [c.flowId, c.status])))
    }

    useEffect(() => {
        getFlows()
            .then(response => showFlows(response))
            .catch(err => console.error(err))
    }, [])

    const search = async () => {
        await showFlows(await getFlows())
    }

    const openFlowDetails = (flow: Flow) => {
        navigate(`/flows/${flow.id}`)
    }

    const context: FlowsContext = {flows, reload: search}

    return (
        <Box style={{display: 'flex', height: '100vh'}}>
            <Box
                w={260}
                p="sm"
                style={{display: 'flex', flexDirection: 'column', borderRight: '1px solid var(--mantine-color-gray-3)'}}
            >
                <Button mb="sm" component={Link} to="/flows/new">+ New flow</Button>
                <ScrollArea style={{flex: 1}}>
                    {flows.map(flow => (
                        <NavLink
                            key={flow.id}
                            label={flow.name}
                            active={flow.id === flowId}
                            onClick={() => openFlowDetails(flow)}
                            leftSection={<StatusDot status={statuses[flow.id]}/>}
                        />
                    ))}
                </ScrollArea>
            </Box>

            <Box style={{flex: 1, minWidth: 0}}>
                <Outlet context={context}/>
            </Box>
        </Box>
    )
}

// /flows without a selected flow: open the first one, or say there are none yet
export const NoFlowSelected = () => {
    const {flows} = useOutletContext<FlowsContext>()

    if (flows.length > 0) {
        return <Navigate to={`/flows/${flows[0].id}`} replace/>
    }

    return <Center h="100vh"><Text c="dimmed">No flows yet. Create one with "New flow".</Text></Center>
}

export default FlowsPage
