package rs.ac.fon.gossip.collector.http.route

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FunctionalityResolverTest {

    private val withoutPatterns = FunctionalityResolver(emptyList())

    @ParameterizedTest
    @CsvSource(
        "/videos/65f1c2a9e4b0a1b2c3d4e5f6,                    /videos/{objectId}",
        "/videos/preview/65f1c2a9e4b0a1b2c3d4e5f6,            /videos/preview/{objectId}",
        "/videos/stream/65F1C2A9E4B0A1B2C3D4E5F6,             /videos/stream/{objectId}",
        "/orders/550e8400-e29b-41d4-a716-446655440000,        /orders/{uuid}",
        "/users/42,                                           /users/{number}",
        "/users/42/videos/65f1c2a9e4b0a1b2c3d4e5f6,           /users/{number}/videos/{objectId}",
    )
    fun replacesIdentifierParts(path: String, route: String) {
        assertThat(withoutPatterns.resolve(path)).isEqualTo(route)
    }

    @ParameterizedTest
    @CsvSource(
        "/videos",
        "/users/principal",
        "/auth/login",
        "/videos/preview/thumbnail",
        "/videos/moj-prvi-video",
        "/api/v1/videos",
        "/videos/abc123",
        "/",
        "/videos/",
    )
    fun keepsPartsThatMightBeEndpoints(path: String) {
        assertThat(withoutPatterns.resolve(path)).isEqualTo(path)
    }

    @Test
    fun leavesTargetsThatAreNotPathsUntouched() {
        assertThat(withoutPatterns.resolve("*")).isEqualTo("*")
    }

    @Test
    fun configuredPatternReplacesMatchingPath() {
        val resolver = resolver("/videos/{slug}")

        assertThat(resolver.resolve("/videos/moj-prvi-video")).isEqualTo("/videos/{slug}")
    }

    @Test
    fun configuredPatternRequiresSameNumberOfParts() {
        val resolver = resolver("/videos/{slug}")

        assertThat(resolver.resolve("/videos/moj-prvi-video/comments"))
            .isEqualTo("/videos/moj-prvi-video/comments")
    }

    @Test
    fun configuredPatternWinsOverIdentifierFormats() {
        val resolver = resolver("/videos/{videoId}")

        assertThat(resolver.resolve("/videos/65f1c2a9e4b0a1b2c3d4e5f6")).isEqualTo("/videos/{videoId}")
    }

    @Test
    fun patternWithMoreLiteralPartsWins() {
        val resolver = resolver("/videos/{slug}", "/videos/thumbnail")

        assertThat(resolver.resolve("/videos/thumbnail")).isEqualTo("/videos/thumbnail")
        assertThat(resolver.resolve("/videos/rodjendan")).isEqualTo("/videos/{slug}")
    }

    @Test
    fun equallySpecificPatternsResolveInDeclarationOrder() {
        val resolver = resolver("/{owner}/videos", "/users/{section}")

        assertThat(resolver.resolve("/users/videos")).isEqualTo("/{owner}/videos")
    }

    @Test
    fun rejectsPatternsThatMatchTheSamePaths() {
        assertThatIllegalArgumentException()
            .isThrownBy { resolver("/videos/{id}", "/videos/{slug}") }
            .withMessageContaining("/videos/{id}")
            .withMessageContaining("/videos/{slug}")
    }

    private fun resolver(vararg patterns: String) = FunctionalityResolver(patterns.map(RoutePattern::parse))
}
