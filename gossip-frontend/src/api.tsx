export interface ServiceRelation {
    id: string
    src: string
    dst: string
    srcProcessName: string | null
    dstProcessName: string | null
    dstPort: number | null
    method: string | null
    url: string | null
    status: number | null
    timestamp: number | null
}

export async function fetchServiceRelations(): Promise<ServiceRelation[]> {
    const response = await fetch('/api/v1/services/calls')

    if (!response.ok) {
        throw new Error(`Error fetching service network relations ${response.status}`)
    }

    return response.json()
}
