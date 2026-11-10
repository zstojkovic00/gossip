import './BottomPanel.css'
import {useState} from 'react'
import {type EdgeData} from '../graph.tsx'

interface Props {
    src: string
    dst: string
    data: EdgeData
    onClose: () => void
}

type SortKey = 'lastSeen' | 'count' | 'errorCount'
type SortDir = 'asc' | 'desc'

function formatTimestamp(ts: number): string {
    return new Date(ts).toLocaleString()
}

export default function BottomPanel({src, dst, data, onClose}: Props) {
    const ports = data.ports.map(p => p.dport)
    const [selectedPort, setSelectedPort] = useState<number | null>(ports[0] ?? null)
    const [sortKey, setSortKey] = useState<SortKey>('lastSeen')
    const [sortDir, setSortDir] = useState<SortDir>('desc')

    const section = data.ports.find(p => p.dport === selectedPort)
    const interactions = (section?.interactions ?? [])
        .slice()
        .sort((a, b) => sortDir === 'asc' ? a[sortKey] - b[sortKey] : b[sortKey] - a[sortKey])

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
                    {section?.connection && (
                        <span>TCP connections: {section.connection.count}</span>
                    )}
                    <select
                        className="bottom-panel__port-select"
                        value={selectedPort ?? ''}
                        onChange={e => setSelectedPort(Number(e.target.value))}
                    >
                        {ports.map(port => (
                            <option key={port} value={port}>{port}</option>
                        ))}
                    </select>
                    <button className="bottom-panel__close" onClick={onClose}>✕</button>
                </div>
            </div>

            {interactions.length === 0
                ? <p className="bottom-panel__empty">No HTTP traffic observed on this port</p>
                : (
                    <div className="bottom-panel__table-wrap">
                        <table className="bottom-panel__table">
                            <thead>
                                <tr>
                                    <th>Method</th>
                                    <th>Path</th>
                                    <th className="sortable" onClick={() => toggleSort('count')}>
                                        Count{sortIndicator('count')}
                                    </th>
                                    <th className="sortable" onClick={() => toggleSort('errorCount')}>
                                        Errors{sortIndicator('errorCount')}
                                    </th>
                                    <th>First seen</th>
                                    <th className="sortable" onClick={() => toggleSort('lastSeen')}>
                                        Last seen{sortIndicator('lastSeen')}
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {interactions.map(i => (
                                    <tr key={i.key}>
                                        <td className="col-method">{i.method}</td>
                                        <td className="col-url">{i.path}</td>
                                        <td className="col-count">{i.count}</td>
                                        <td className={`col-count ${i.errorCount > 0 ? 'col-errors' : ''}`}>{i.errorCount}</td>
                                        <td className="col-time">{formatTimestamp(i.firstSeen)}</td>
                                        <td className="col-time">{formatTimestamp(i.lastSeen)}</td>
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
