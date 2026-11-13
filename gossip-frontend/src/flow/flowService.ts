import axios from 'axios'
import type {Conformance, Flow, FlowRequest} from './types.ts'

export const getFlows = () =>
    axios.get<Flow[]>('/api/v1/flows')
        .then(response => response.data)

export const getFlow = (id: string) =>
    axios.get<Flow>(`/api/v1/flows/${id}`)
        .then(response => response.data)

export const createFlow = (request: FlowRequest) =>
    axios.post<Flow>('/api/v1/flows', request)
        .then(response => response.data)

export const updateFlow = (id: string, request: FlowRequest) =>
    axios.put<Flow>(`/api/v1/flows/${id}`, request)
        .then(response => response.data)

export const deleteFlow = (id: string) =>
    axios.delete(`/api/v1/flows/${id}`)

export const checkConformance = (id: string) =>
    axios.get<Conformance>(`/api/v1/flows/${id}/conformance`)
        .then(response => response.data)

export const errorMessage = (err: unknown) =>
    axios.isAxiosError(err) ? err.response?.data?.detail ?? err.message : String(err)
