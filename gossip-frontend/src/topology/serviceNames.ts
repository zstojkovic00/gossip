// Demo only: streaming-service runs with fixed IP addresses (see its docker-compose.yml),
// so the frontend can show which service is behind each address.
const DEMO_NAMES: Record<string, string> = {
    '172.19.0.1': 'client',
    '172.19.0.2': 'discovery-server',
    '172.19.0.3': 'mysql',
    '172.19.0.4': 'mongodb',
    '172.19.0.5': 'kafka',
    '172.19.0.6': 'api-gateway',
    '172.19.0.7': 'video-service',
    '172.19.0.8': 'frontend',
    '172.19.0.9': 'security-service',
}

export function serviceName(ip: string): string | null {
    return DEMO_NAMES[ip] ?? null
}
