import type {ConformanceStatus, Step} from './types.ts'

// Worst first, so an edge shared by several steps shows the worst of them
export const CONFORMANCE_SEVERITY: ConformanceStatus[] = ['MISSING', 'FAILING', 'OBSERVED']

const STATUS_COLORS: Record<ConformanceStatus, string> = {
    OBSERVED: 'green',
    FAILING: 'orange',
    MISSING: 'red',
}

// Mantine color name, for badges
export const getStatusColor = (status: ConformanceStatus) => STATUS_COLORS[status]

// CSS color, for dots, borders and edges; gray when the status is not known yet
export const getStatusCssColor = (status: ConformanceStatus | null | undefined) =>
    status ? `var(--mantine-color-${STATUS_COLORS[status]}-6)` : 'var(--mantine-color-gray-4)'

export const getStepOperation = (step: Step) =>
    step.protocol === 'HTTP' ? `${step.method} ${step.path}` : 'TCP'

export const getStepRoute = (step: Step) =>
    `${step.caller} → ${step.ip}:${step.port}`
