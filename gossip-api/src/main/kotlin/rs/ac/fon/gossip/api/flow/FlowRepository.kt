package rs.ac.fon.gossip.api.flow

import org.neo4j.driver.Driver
import org.neo4j.driver.Record
import org.neo4j.driver.TransactionContext
import org.neo4j.driver.Value
import org.neo4j.driver.Values
import org.springframework.stereotype.Repository

@Repository
class FlowRepository(
    private val neo4jDriver: Driver
) {
    fun findAll(): List<Flow> = find("").map { it.toFlow() }

    fun findById(id: String): Flow? = find("WHERE f.id = \$id", "id", id).singleOrNull()?.toFlow()

    fun save(flow: Flow) {
        val parameters = parameters(flow)

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                createFlow(tx, parameters)
                createSteps(tx, parameters)
                linkSteps(tx, parameters)
            }
        }
    }

    fun update(flow: Flow) {
        val parameters = parameters(flow)

        neo4jDriver.session().use { session ->
            session.executeWrite { tx ->
                recreateFlow(tx, parameters)
                createSteps(tx, parameters)
                linkSteps(tx, parameters)
            }
        }
    }

    private fun createFlow(tx: TransactionContext, parameters: Value) {
        tx.run(
            """
            CREATE (:Flow {id: ${'$'}id, name: ${'$'}name})
            """.trimIndent(), parameters
        ).consume()
    }

    private fun recreateFlow(tx: TransactionContext, parameters: Value) {
        tx.run(
            """
            MATCH (f:Flow {id: ${'$'}id})
            SET f.name = ${'$'}name
            WITH f
            OPTIONAL MATCH (f)-[:HAS_STEP]->(old:Step)
            DETACH DELETE old
            """.trimIndent(), parameters
        ).consume()
    }

    private fun createSteps(tx: TransactionContext, parameters: Value) {
        tx.run(
            """
            MATCH (f:Flow {id: ${'$'}id})
            UNWIND ${'$'}steps AS step
            CREATE (f)-[:HAS_STEP]->(:Step {
                id: step.id, caller: step.caller, ip: step.ip, port: step.port,
                method: step.method, path: step.path
            })
            """.trimIndent(), parameters
        ).consume()
    }

    private fun linkSteps(tx: TransactionContext, parameters: Value) {
        tx.run(
            """
            MATCH (f:Flow {id: ${'$'}id})
            UNWIND ${'$'}steps AS step
            UNWIND step.after AS previous
            MATCH (f)-[:HAS_STEP]->(from:Step {id: previous})
            MATCH (f)-[:HAS_STEP]->(to:Step {id: step.id})
            CREATE (from)-[:NEXT]->(to)
            """.trimIndent(), parameters
        ).consume()
    }

    private fun parameters(flow: Flow): Value {
        val steps = flow.steps.map {
            mapOf(
                "id" to it.id, "caller" to it.caller, "ip" to it.ip, "port" to it.port,
                "method" to it.method, "path" to it.path, "after" to it.after,
            )
        }
        return Values.parameters("id", flow.id, "name", flow.name, "steps", steps)
    }

    fun delete(id: String) {
        val query = """
            MATCH (f:Flow {id: ${'$'}id})
            OPTIONAL MATCH (f)-[:HAS_STEP]->(s:Step)
            DETACH DELETE f, s
        """.trimIndent()

        neo4jDriver.executableQuery(query).withParameters(mapOf("id" to id)).execute()
    }

    private fun find(where: String, vararg parameters: Any): List<Record> {
        val query = """
            MATCH (f:Flow)-[:HAS_STEP]->(s:Step)
            $where
            WITH f, s, [(previous:Step)-[:NEXT]->(s) | previous.id] AS after
            ORDER BY s.id
            RETURN f.id AS id, f.name AS name, collect({
                id: s.id, caller: s.caller, ip: s.ip, port: s.port,
                method: s.method, path: s.path, after: after
            }) AS steps
            ORDER BY name
        """.trimIndent()

        return neo4jDriver.executableQuery(query)
            .withParameters(
                Values.parameters(*parameters)
                    .asMap()
            ).execute().records()
    }

    private fun Record.toFlow() = Flow(
        id = this["id"].asString(),
        name = this["name"].asString(),
        steps = this["steps"].asList { s ->
            Step(
                id = s["id"].asString(),
                caller = s["caller"].asString(),
                ip = s["ip"].asString(),
                port = s["port"].asLong(),
                method = s["method"].asString(null),
                path = s["path"].asString(null),
                after = s["after"].asList { it.asString() }.sorted(),
            )
        },
    )
}
