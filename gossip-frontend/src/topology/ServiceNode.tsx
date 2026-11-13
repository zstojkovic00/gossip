import {Handle, Position, type Node, type NodeProps} from '@xyflow/react'
import {Paper, Text} from '@mantine/core'
import {serviceName} from './serviceNames.ts'

export const SERVICE_WIDTH = 200
export const SERVICE_HEIGHT = 76

export type ServiceNodeData = {
    ip: string
    ports: number[]
}

export type ServiceNodeType = Node<ServiceNodeData, 'service'>

// One server on a graph, used by both the topology and the flows
const ServiceNode = ({data, selected}: NodeProps<ServiceNodeType>) => {
    const name = serviceName(data.ip)

    return (
        <Paper
            withBorder
            shadow={selected ? 'md' : 'xs'}
            p="xs"
            w={SERVICE_WIDTH}
            h={SERVICE_HEIGHT}
            style={{
                borderLeft: '4px solid var(--mantine-color-blue-6)',
                outline: selected ? '2px solid var(--mantine-color-blue-4)' : undefined,
            }}
        >
            <Handle type="target" position={Position.Top} style={{opacity: 0}}/>
            <Text size="sm" fw={700} truncate>{name ?? data.ip}</Text>
            {name && <Text size="xs" ff="monospace">{data.ip}</Text>}
            <Text size="xs" c="dimmed" mt={2} truncate>
                {data.ports.length > 0 ? `ports ${data.ports.join(', ')}` : 'client'}
            </Text>
            <Handle type="source" position={Position.Bottom} style={{opacity: 0}}/>
        </Paper>
    )
}

export default ServiceNode
