package rs.ac.fon.gossip.api.flow

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test

class FlowValidationTest {

    @Test
    fun acceptsParallelStepsThatJoinAgain() {
        val flow = flow(step("s1"), step("s2", "s1"), step("s3", "s1"), step("s4", "s2", "s3"))

        assertThatCode { validate(flow) }.doesNotThrowAnyException()
    }

    @Test
    fun acceptsTcpOnlyStep() {
        val flow = flow(step("s1").copy(method = null, path = null))

        assertThatCode { validate(flow) }.doesNotThrowAnyException()
    }

    @Test
    fun rejectsBlankName() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1")).copy(name = " ")) }
            .withMessageContaining("name")
    }

    @Test
    fun rejectsFlowWithoutSteps() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow()) }
            .withMessageContaining("at least one step")
    }

    @Test
    fun rejectsDuplicateStepIds() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1"), step("s1"))) }
            .withMessageContaining("Duplicate step ids: [s1]")
    }

    @Test
    fun rejectsStepAfterUnknownStep() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1", "s9"))) }
            .withMessageContaining("unknown step 's9'")
    }

    @Test
    fun rejectsMethodWithoutPath() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1").copy(path = null))) }
            .withMessageContaining("both method and path")
    }

    private fun flow(vararg steps: Step) = Flow("f1", "Gledanje videa", steps.toList())

    private fun step(id: String, vararg after: String) =
        Step(id, "172.19.0.6", "172.19.0.7", 8081, "GET", "/videos/{objectId}", after.toList())
}
