import axios from 'axios'
import type {ServiceOperations, Topology} from './types.ts'

export const getTopology = () =>
    axios.get<Topology>('/api/v1/topology').then(response => response.data)

export const getOperations = () =>
    axios.get<{ services: ServiceOperations[] }>('/api/v1/topology/operations').then(response => response.data.services)
