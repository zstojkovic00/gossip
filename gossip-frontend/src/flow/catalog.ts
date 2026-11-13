import type {Operation, ServiceOperations, Topology} from '../topology/types.ts'
import {serviceName} from '../topology/serviceNames.ts'

export interface CatalogEntry {
    ip: string
    port: number
    operations: Operation[] | null
}

export function buildCatalog(operations: ServiceOperations[], topology: Topology): CatalogEntry[] {
    const httpEntries = operations.map(
        service => ({ip: service.ip, port: service.port, operations: service.operations}))

    const httpOwners = new Set(operations.map(service => `${service.ip}:${service.port}`))
    const tcpEntries = [...new Set(topology.connections.map(c => `${c.dst}:${c.dport}`))]
        .filter(target => !httpOwners.has(target))
        .map(target => {
            const [ip, port] = target.split(':')
            return {ip, port: Number(port), operations: null}
        })

    return [...httpEntries, ...tcpEntries].sort((a, b) => `${a.ip}:${a.port}`.localeCompare(`${b.ip}:${b.port}`))
}

export function filterCatalog(catalog: CatalogEntry[], search: string): CatalogEntry[] {
    const text = search.trim().toLowerCase()
    if (text === '') return catalog

    return catalog.flatMap(entry => {
        if (`${serviceName(entry.ip) ?? ''} ${entry.ip}:${entry.port}`.toLowerCase().includes(text)) return [entry]
        if (entry.operations === null) return []
        const operations = entry.operations.filter(o => `${o.method} ${o.path}`.toLowerCase().includes(text))
        return operations.length > 0 ? [{...entry, operations}] : []
    })
}
