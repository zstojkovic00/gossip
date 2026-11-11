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
    fun acceptsTcpStepWithoutMethodAndPath() {
        val flow = flow(step("s1").copy(protocol = Protocol.TCP, method = null, path = null))

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
    fun rejectsHttpStepWithoutPath() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1").copy(path = null))) }
            .withMessageContaining("HTTP step 's1' must have method and path")
    }

    @Test
    fun rejectsTcpStepWithMethodAndPath() {
        assertThatIllegalArgumentException()
            .isThrownBy { validate(flow(step("s1").copy(protocol = Protocol.TCP))) }
            .withMessageContaining("TCP step 's1' must not have method or path")
    }

    private fun flow(vararg steps: Step) = Flow("f1", "Get videos", steps.toList())

    private fun step(id: String, vararg after: String) =
        Step(id, Protocol.HTTP, "172.19.0.6", "172.19.0.7", 8081, "GET", "/videos/{objectId}", after.toList())
}
