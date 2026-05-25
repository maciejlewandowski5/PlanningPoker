package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.io.path.createTempFile
import kotlin.test.*

class ServerTest {

    private fun ApplicationTestBuilder.setup() {
        val tempDb = createTempFile("planningpoker_test", ".db").toFile()
        environment {
            config = MapApplicationConfig("database.url" to "jdbc:sqlite:${tempDb.absolutePath}")
        }
        application {
            configureDatabase()
            configureHttp()
            configureDependencyInjection()
            configureRouting()
        }
    }

    @Test
    fun `create room returns 201 with 6-char code`() = testApplication {
        setup()
        val response = client.post("/rooms")
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["roomId"])
        assertEquals(6, body["code"]!!.jsonPrimitive.content.length)
    }

    @Test
    fun `join room with valid code returns participant id`() = testApplication {
        setup()
        val createBody = Json.parseToJsonElement(client.post("/rooms").bodyAsText()).jsonObject
        val code = createBody["code"]!!.jsonPrimitive.content

        val joinResponse = client.post("/rooms/$code/join") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Alice"}""")
        }
        assertEquals(HttpStatusCode.OK, joinResponse.status)
        val joinBody = Json.parseToJsonElement(joinResponse.bodyAsText()).jsonObject
        assertNotNull(joinBody["participantId"])
        assertEquals(code, joinBody["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `join room with invalid code returns 404`() = testApplication {
        setup()
        val response = client.post("/rooms/BADCOD/join") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Bob"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
