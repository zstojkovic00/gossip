import './BottomPanel.css'
import {useState} from 'react'
import {type EdgeData} from '../graph.tsx'

interface Props {
    src: string
    dst: string
    data: EdgeData
    onClose: () => void
}

type SortKey = 'timestamp' | 'status'
type SortDir = 'asc' | 'desc'

function statusClass(status: number): string {
    if (status >= 500) return 'status--5xx'
    if (status >= 400) return 'status--4xx'
    return 'status--2xx'
}

function formatTimestamp(ts: number | null): string {
    if (ts === null) return '—'
    return new Date(ts).toLocaleTimeString()
}

export default function BottomPanel({src, dst, data, onClose}: Props) {
    const ports = data.ports.map(p => p.dstPort)
    const [selectedPort, setSelectedPort] = useState<number | null>(ports[0] ?? null)
    const [sortKey, setSortKey] = useState<SortKey>('timestamp')
    const [sortDir, setSortDir] = useState<SortDir>('desc')

    const calls = (data.ports.find(p => p.dstPort === selectedPort)?.calls ?? [])
        .slice()
        .sort((a, b) => {
            const av = sortKey === 'timestamp' ? (a.timestamp ?? 0) : a.status
            const bv = sortKey === 'timestamp' ? (b.timestamp ?? 0) : b.status
            return sortDir === 'asc' ? av - bv : bv - av
        })

    function toggleSort(key: SortKey) {
        if (sortKey === key) {
            setSortDir(d => d === 'asc' ? 'desc' : 'asc')
        } else {
            setSortKey(key)
            setSortDir('desc')
        }
    }

    function sortIndicator(key: SortKey) {
        if (sortKey !== key) return ' ↕'
        return sortDir === 'asc' ? ' ↑' : ' ↓'
    }

    return (
        <div className="bottom-panel">
            <div className="bottom-panel__header">
                <div className="bottom-panel__title">
                    <span className="bottom-panel__ip">{src}</span>
                    <span className="bottom-panel__arrow">→</span>
                    <span className="bottom-panel__ip">{dst}</span>
                </div>
                <div className="bottom-panel__controls">
                    <select
                        className="bottom-panel__port-select"
                        value={selectedPort ?? ''}
                        onChange={e => setSelectedPort(e.target.value ? Number(e.target.value) : null)}
                    >
                        {ports.map(port => (
                            <option key={port} value={port ?? ''}>{port ?? '—'}</option>
                        ))}
                    </select>
                    <button className="bottom-panel__close" onClick={onClose}>✕</button>
                </div>
            </div>

            {calls.length === 0
                ? <p className="bottom-panel__empty">No HTTP traffic observed on this port</p>
                : (
                    <div className="bottom-panel__table-wrap">
                        <table className="bottom-panel__table">
                            <thead>
                                <tr>
                                    <th className="sortable" onClick={() => toggleSort('timestamp')}>
                                        Time{sortIndicator('timestamp')}
                                    </th>
                                    <th>Method</th>
                                    <th>URL</th>
                                    <th className="sortable" onClick={() => toggleSort('status')}>
                                        Status{sortIndicator('status')}
                                    </th>
                                    <th>Payload</th>
                                </tr>
                            </thead>
                            <tbody>
                                {calls.map((call, i) => (
                                    <tr key={i}>
                                        <td className="col-time">{formatTimestamp(call.timestamp)}</td>
                                        <td className="col-method">{call.method}</td>
                                        <td className="col-url">{call.url}</td>
                                        <td className={`col-status ${statusClass(call.status)}`}>{call.status}</td>
                                        <td className="col-payload">—</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )
            }
        </div>
    )
}
