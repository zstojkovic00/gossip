package rs.ac.fon.gossip.collector.http.route

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RoutePatternTest {

    @Test
    fun parameterMatchesAnySinglePart() {
        val pattern = RoutePattern.parse("/videos/{slug}/comments")
        assertThat(pattern.matches(listOf("videos", "test", "comments"))).isTrue()
        assertThat(pattern.matches(listOf("videos", "test", "likes"))).isFalse()
    }

    @Test
    fun countsLiteralParts() {
        assertThat(RoutePattern.parse("/videos/{slug}/comments").literalCount).isEqualTo(2)
    }

    @Test
    fun rejectsPatternWithoutLeadingSlash() {
        assertThatIllegalArgumentException()
            .isThrownBy { RoutePattern.parse("videos/{slug}") }
            .withMessageContaining("must start with '/'")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "/videos/", "/videos//comments"])
    fun rejectsEmptyParts(template: String) {
        assertThatIllegalArgumentException()
            .isThrownBy { RoutePattern.parse(template) }
            .withMessageContaining("empty part")
    }

    @ParameterizedTest
    @ValueSource(strings = ["/videos/{slug", "/videos/slug}", "/videos/{}", "/videos/pre{slug}", "/videos/{1st}"])
    fun rejectsMalformedParameters(template: String) {
        assertThatIllegalArgumentException()
            .isThrownBy { RoutePattern.parse(template) }
            .withMessageContaining("malformed parameter")
    }
}
