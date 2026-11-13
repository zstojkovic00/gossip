import {useState} from 'react'
import {CloseButton, Group, Pagination, Paper, Select, Table, Text, TextInput, UnstyledButton} from '@mantine/core'
import {IconArrowRight, IconChevronDown, IconChevronUp, IconSearch, IconSelector} from '@tabler/icons-react'
import type {Interaction} from './types.ts'
import type {EdgeData} from './topologyGraph.ts'

interface Props {
    src: string
    dst: string
    data: EdgeData
    onClose: () => void
}

type SortKey = 'count' | 'errorCount' | 'lastSeen'

const PAGE_SIZE = 10

// Most called first, and among equally called the most recent first
function compare(a: Interaction, b: Interaction, key: SortKey, descending: boolean): number {
    const difference = descending ? b[key] - a[key] : a[key] - b[key]
    return difference !== 0 ? difference : b.lastSeen - a.lastSeen
}

// Interactions on one edge of the topology, one port at a time
const BottomPanel = ({src, dst, data, onClose}: Props) => {
    const ports = data.ports.map(p => p.dport)
    const [selectedPort, setSelectedPort] = useState<number | null>(ports[0] ?? null)
    const [filter, setFilter] = useState('')
    const [sortKey, setSortKey] = useState<SortKey>('count')
    const [descending, setDescending] = useState(true)
    const [page, setPage] = useState(1)

    const section = data.ports.find(p => p.dport === selectedPort)
    const interactions = (section?.interactions ?? [])
        .filter(i => i.path.toLowerCase().includes(filter.toLowerCase()))
        .sort((a, b) => compare(a, b, sortKey, descending))

    const pageCount = Math.max(1, Math.ceil(interactions.length / PAGE_SIZE))
    const currentPage = Math.min(page, pageCount)
    const rows = interactions.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE)

    const sortBy = (key: SortKey) => {
        if (sortKey === key) {
            setDescending(!descending)
        } else {
            setSortKey(key)
            setDescending(true)
        }
        setPage(1)
    }

    const sortableHeader = (key: SortKey, label: string) => {
        const Icon = sortKey !== key ? IconSelector : descending ? IconChevronDown : IconChevronUp
        return (
            <Table.Th key={key}>
                <UnstyledButton onClick={() => sortBy(key)}>
                    <Group gap={4} wrap="nowrap">
                        <Text fw={600} ff="monospace" fz={13}>{label}</Text>
                        <Icon size={16} stroke={2} color={sortKey === key ? 'var(--mantine-color-blue-6)' : 'var(--mantine-color-gray-5)'}/>
                    </Group>
                </UnstyledButton>
            </Table.Th>
        )
    }

    return (
        <Paper
            shadow="md"
            withBorder
            style={{position: 'absolute', bottom: 0, left: 0, right: 0, height: 360, zIndex: 10, display: 'flex', flexDirection: 'column'}}
        >
            <Group justify="space-between" px="md" py="xs" style={{borderBottom: '1px solid var(--mantine-color-gray-3)'}}>
                <Group gap="xs">
                    <Text ff="monospace" fz={13} fw={700}>{src}</Text>
                    <IconArrowRight size={16} color="var(--mantine-color-gray-5)"/>
                    <Text ff="monospace" fz={13} fw={700}>{dst}</Text>
                </Group>
                <Group gap="md">
                    {section?.connection && (
                        <Text ff="monospace" fz={13}>TCP connections: {section.connection.count}</Text>
                    )}
                    <Select
                        size="xs"
                        styles={{input: {fontFamily: 'monospace', fontSize: 13}}}
                        w={110}
                        data={ports.map(port => ({value: String(port), label: `port ${port}`}))}
                        value={selectedPort === null ? null : String(selectedPort)}
                        onChange={value => {
                            setSelectedPort(value === null ? null : Number(value))
                            setPage(1)
                        }}
                        allowDeselect={false}
                    />
                    <CloseButton onClick={onClose}/>
                </Group>
            </Group>

            <TextInput
                size="xs"
                w={260}
                mx="md"
                my="xs"
                leftSection={<IconSearch size={14}/>}
                styles={{input: {fontFamily: 'monospace', fontSize: 13}}}
                placeholder="Filter path"
                value={filter}
                onChange={event => {
                    setFilter(event.currentTarget.value)
                    setPage(1)
                }}
            />

            <Table.ScrollContainer minWidth={600} style={{flex: 1}}>
                <Table stickyHeader highlightOnHover ff="monospace" fz={13}>
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Method</Table.Th>
                            <Table.Th>Path</Table.Th>
                            {sortableHeader('count', 'Count')}
                            {sortableHeader('errorCount', 'Errors')}
                            {sortableHeader('lastSeen', 'Last seen')}
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {rows.map(i => (
                            <Table.Tr key={i.key}>
                                <Table.Td><Text ff="monospace" fz={13} fw={700} c="blue">{i.method}</Text></Table.Td>
                                <Table.Td><Text ff="monospace" fz={13}>{i.path}</Text></Table.Td>
                                <Table.Td>{i.count}</Table.Td>
                                <Table.Td>{i.errorCount}</Table.Td>
                                <Table.Td><Text ff="monospace" fz={13} c="dimmed">{new Date(i.lastSeen).toLocaleString()}</Text></Table.Td>
                            </Table.Tr>
                        ))}
                    </Table.Tbody>
                </Table>
                {rows.length === 0 && (
                    <Text c="dimmed" ff="monospace" fz={13} fs="italic" p="md">
                        {filter ? 'No path matches the filter' : 'No HTTP traffic observed on this port'}
                    </Text>
                )}
            </Table.ScrollContainer>

            <Group justify="space-between" px="md" py="xs" style={{borderTop: '1px solid var(--mantine-color-gray-3)'}}>
                <Text ff="monospace" fz={13} c="dimmed">
                    {interactions.length === 0
                        ? '0 rows'
                        : `${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, interactions.length)} of ${interactions.length}`}
                </Text>
                <Pagination size="sm" total={pageCount} value={currentPage} onChange={setPage}/>
            </Group>
        </Paper>
    )
}

export default BottomPanel
