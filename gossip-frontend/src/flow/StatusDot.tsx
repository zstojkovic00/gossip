import type {ConformanceStatus} from './types.ts'
import {getStatusCssColor} from './status.ts'

interface Props {
    status: ConformanceStatus | null | undefined
    size?: number
}

const StatusDot = ({status, size = 10}: Props) => (
    <span style={{
        display: 'inline-block',
        width: size,
        height: size,
        borderRadius: '50%',
        flexShrink: 0,
        background: getStatusCssColor(status),
    }}/>
)

export default StatusDot
