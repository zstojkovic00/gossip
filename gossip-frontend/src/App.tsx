import {NavLink as RouterLink, Navigate, Route, Routes, useLocation} from 'react-router'
import {AppShell, NavLink, Title} from '@mantine/core'
import TopologyPage from './topology/TopologyPage.tsx'
import FlowsPage, {NoFlowSelected} from './flow/FlowsPage.tsx'
import FlowDetailsPage from './flow/FlowDetailsPage.tsx'
import FlowEditorPage from './flow/FlowEditorPage.tsx'

const App = () => {
    const location = useLocation()

    return (
        <AppShell navbar={{width: 200, breakpoint: 'xs'}} padding={0}>
            <AppShell.Navbar p="sm">
                <Title order={4} px="xs" mb="md">Gossip</Title>
                <NavLink component={RouterLink} to="/topology"
                         label="Topology" active={location.pathname.startsWith('/topology')}/>
                <NavLink component={RouterLink} to="/flows"
                         label="Flows" active={location.pathname.startsWith('/flows')}/>
            </AppShell.Navbar>

            <AppShell.Main>
                <Routes>
                    <Route path="/" element={<Navigate to="/topology" replace/>}/>
                    <Route path="/topology" element={<TopologyPage/>}/>
                    <Route path="/flows" element={<FlowsPage/>}>
                        <Route index element={<NoFlowSelected/>}/>
                        <Route path="new" element={<FlowEditorPage/>}/>
                        <Route path=":flowId" element={<FlowDetailsPage/>}/>
                        <Route path=":flowId/edit" element={<FlowEditorPage/>}/>
                    </Route>
                </Routes>
            </AppShell.Main>
        </AppShell>
    )
}

export default App
