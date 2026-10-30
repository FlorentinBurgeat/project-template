package com.template

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.template.shared.PostgresTestConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The back/front contract: `/v3/api-docs` must equal the committed `openapi/template-api.json`, from which the
 * front generates its types. After an intended API change, refresh the file with
 * `./mvnw test -Dtest=OpenApiContractTest -Dcontract.update=true` and commit it.
 */
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig::class)
class OpenApiContractTest @Autowired constructor(private val mockMvc: MockMvc) {

    private val mapper = ObjectMapper()
    private val contractFile: Path = Path.of("openapi", "template-api.json")

    @Test
    fun `the published API matches the committed contract`() {
        val actual = generatedContract()

        if (System.getProperty("contract.update") == "true") {
            Files.createDirectories(contractFile.parent)
            Files.writeString(contractFile, actual)
            return
        }

        if (!Files.exists(contractFile)) {
            fail("$contractFile does not exist. Generate it with: ./mvnw test -Dtest=OpenApiContractTest -Dcontract.update=true")
        }
        val committed = Files.readString(contractFile).normalizeLineEndings()
        assertEquals(
            committed,
            actual,
            "The API no longer matches $contractFile. If the change is intended, refresh the contract with: " +
                "./mvnw test -Dtest=OpenApiContractTest -Dcontract.update=true, then commit it."
        )
    }

    @Test
    fun `the contract publishes the public auth operations with their wire names`() {
        val tree = generatedTree()
        val schemas = tree.path("components").path("schemas")
        val paths = tree.path("paths")

        assertEquals(
            setOf("/api/auth/login", "/api/auth/register", "/api/auth/callback", "/api/auth/refresh", "/api/auth/logout"),
            paths.fieldNames().asSequence().toSet(),
            "Only /api/** is published: no actuator, no error endpoint"
        )
        assertEquals("getLoginUrl", operation(paths, "/api/auth/login", "get").path("operationId").asText())
        assertEquals("getRegistrationUrl", operation(paths, "/api/auth/register", "get").path("operationId").asText())
        assertEquals("handleAuthCallback", operation(paths, "/api/auth/callback", "post").path("operationId").asText())
        assertEquals("refreshToken", operation(paths, "/api/auth/refresh", "post").path("operationId").asText())
        assertEquals("logout", operation(paths, "/api/auth/logout", "post").path("operationId").asText())

        assertEquals(setOf("access_token", "expires_in", "token_type"), required(schemas, "PublicTokenResponse"))
        assertEquals(setOf("access_token", "expires_in", "token_type"), properties(schemas, "PublicTokenResponse"))
        assertEquals(setOf("redirect_url"), required(schemas, "LoginRedirectResponse"))
        assertEquals(setOf("code", "state"), required(schemas, "CallbackRequest"))
        assertEquals(setOf("code", "state", "redirect_uri"), properties(schemas, "CallbackRequest"))
        assertEquals(setOf("timestamp", "status", "error", "message", "errors"), required(schemas, "AuthErrorResponse"))
        assertTrue(schemas.fieldNames().asSequence().none { it == "TokenResponse" || it == "AuthorizationRequest" }, "the refresh token must not leak into the contract")

        paths.fieldNames().asSequence().forEach { path ->
            paths.path(path).fields().forEach { (_, op) ->
                assertTrue(op.path("parameters").none { it.path("in").asText() == "cookie" }, "$path: cookies are described in prose only")
            }
        }
        val queryParams = operation(paths, "/api/auth/login", "get").path("parameters").map { it.path("name").asText() }
        assertEquals(listOf("redirect_uri", "locale"), queryParams)
        assertEquals(setOf("200", "400"), operation(paths, "/api/auth/login", "get").path("responses").fieldNames().asSequence().toSet())
        assertEquals(setOf("200", "400", "401", "503"), operation(paths, "/api/auth/callback", "post").path("responses").fieldNames().asSequence().toSet())
        assertEquals(setOf("200", "401", "503"), operation(paths, "/api/auth/refresh", "post").path("responses").fieldNames().asSequence().toSet())
        assertEquals(setOf("204"), operation(paths, "/api/auth/logout", "post").path("responses").fieldNames().asSequence().toSet())
    }

    @Test
    fun `the bearer scheme is published and the auth operations are public`() {
        val tree = generatedTree()
        val paths = tree.path("paths")

        val scheme = tree.path("components").path("securitySchemes").path("bearerAuth")
        assertEquals("http", scheme.path("type").asText())
        assertEquals("bearer", scheme.path("scheme").asText())
        assertEquals("JWT", scheme.path("bearerFormat").asText())

        // A protected controller declares @SecurityRequirement(name = BEARER_AUTH): assert its operations here too.
        listOf("/api/auth/login" to "get", "/api/auth/register" to "get", "/api/auth/callback" to "post", "/api/auth/refresh" to "post", "/api/auth/logout" to "post")
            .forEach { (path, method) -> assertTrue(operation(paths, path, method).path("security").isMissingNode, "$method $path is public") }
    }

    private fun operation(paths: JsonNode, path: String, method: String): JsonNode = paths.path(path).path(method)

    private fun properties(schemas: JsonNode, name: String): Set<String> =
        schemas.path(name).path("properties").fieldNames().asSequence().toSet()

    private fun required(schemas: JsonNode, name: String): Set<String> =
        schemas.path(name).path("required").map { it.asText() }.toSet()

    private fun generatedTree(): JsonNode {
        val body = mockMvc.perform(get("/v3/api-docs").with(jwt()))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val tree = mapper.readTree(body)
        // `servers` is derived from the request (host, port): not part of the contract.
        (tree as ObjectNode).remove("servers")
        return tree
    }

    /** Sorted keys and fixed pretty printing, whatever springdoc's writer does, so the file is deterministic. */
    private fun generatedContract(): String {
        val sorted = mapper.readValue(mapper.writeValueAsString(generatedTree()), Any::class.java)
        val json = mapper.copy()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(sorted)
        return json.normalizeLineEndings() + "\n"
    }

    private fun String.normalizeLineEndings() = replace("\r\n", "\n")
}
