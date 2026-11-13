import dagre from '@dagrejs/dagre'

export interface Link {
    source: string
    target: string
}

// Places nodes top to bottom by their links (callers above the services they call),
// with fewer crossing edges. Returns the top left corner of each node, as React Flow expects.
export function layoutTopToBottom(
    ids: string[],
    links: Link[],
    width: number,
    height: number,
    rankSeparation = 70,
): Map<string, { x: number, y: number }> {
    const graph = new dagre.graphlib.Graph()
    graph.setGraph({rankdir: 'TB', nodesep: 40, ranksep: rankSeparation})
    graph.setDefaultEdgeLabel(() => ({}))

    ids.forEach(id => graph.setNode(id, {width, height}))
    links.forEach(link => graph.setEdge(link.source, link.target))

    dagre.layout(graph)

    // dagre gives the center of a node
    return new Map(ids.map(id => {
        const {x, y} = graph.node(id)
        return [id, {x: x - width / 2, y: y - height / 2}]
    }))
}
