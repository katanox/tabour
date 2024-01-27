package com.katanox.tabour.proto.mapper

import com.katanox.tabour.person.Hello.Person
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.Message

class ProtoMapperTest {
    private val baseBody =
        // language=json
        """
       {
           "name": "Wutang",
           "id": 1,
           "email": "isfor@thechildren.com"
       }
        """
            .trimIndent()

    private fun createMessage(body: String = baseBody) = Message.builder().body(body).build()

    @Test
    fun fromSqsMessage() {
        val expected =
            Person.newBuilder().setName("Wutang").setId(1).setEmail("isfor@thechildren.com").build()

        val person: Person = Person.newBuilder().fromSqsMessage(createMessage())

        assertEquals(expected, person)
    }

    @Test
    fun fromSqsMessageOrNull() {
        val wrongBody =
            """
       {
            "wrong": "Wutang",
            "key": 1,
       }
    """
                .trimIndent()

        assertNull(
            Person.newBuilder()
                .fromSqsMessageOrNull<Person.Builder, Person>(createMessage(wrongBody))
        )
    }

    @Test
    fun fromSqsMessageOrNullWithExceptionHandler() {
        val wrongBody =
            """
       {
            "wrong": "Wutang",
            "key": 1,
       }
    """
                .trimIndent()
        var errorMessage: String? = null

        Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person>(createMessage(wrongBody)) {
            errorMessage = it.message
        }

        assertNotNull(errorMessage)
        assertTrue(errorMessage?.isNotEmpty() ?: false)
    }

    @Test
    fun fromSqsMessageOrNullWithMapper() {
        val errorHandler: (Throwable) -> Unit = {}

        val nameLength =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person, Int>(
                createMessage(),
                errorHandler
            ) {
                it.name.length
            }

        assertNotNull(nameLength)
        assertEquals(6, nameLength)
    }

    @Test
    fun fromSqsMessageOrNullWithMapperAndErrorHandler() {
        val wrongBody =
            """
       {
            "wrong": "Wutang",
            "key": 1,
       }
    """
                .trimIndent()
        var errorMessage: String? = null
        val errorHandler: (Throwable) -> Unit = { errorMessage = it.message }

        val nameLength =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person, Int>(
                createMessage(wrongBody),
                errorHandler
            ) {
                it.name.length
            }

        assertNotNull(errorMessage)
        assertNull(nameLength)
    }

    @Test
    fun jsonify() {
        val expected =
            // language=json
            """
       {
         "name": "Wutang",
         "id": 1,
         "email": "isfor@thechildren.com"
       }
        """
                .trimIndent()

        val jsonified =
            Person.newBuilder()
                .setName("Wutang")
                .setId(1)
                .setEmail("isfor@thechildren.com")
                .build()
                .jsonify()
                .trimIndent()

        assertEquals(expected, jsonified)
    }
}
