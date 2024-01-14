package com.katanox.tabour.proto

import com.katanox.tabour.Hello.Person
import com.katanox.tabour.proto.mapper.fromSqsMessage
import com.katanox.tabour.proto.mapper.fromSqsMessageOrNull
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.Message

class ProtoMapperTest {

    @Test
    fun fromSqsMessage() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
                "email": "isfor@thechildren.com"
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()
        val expected =
            Person.newBuilder().setName("Wutang").setId(1).setEmail("isfor@thechildren.com").build()

        val person: Person = Person.newBuilder().fromSqsMessage(message)

        assertEquals(expected, person)
    }

    @Test
    fun fromSqsMessageOrNull() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()

        val person: Person? =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person>(message)

        assertNull(person)
    }

    @Test
    fun fromSqsMessageOrNullWithErrorHandler() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()
        var errorCounter = 0

        val person: Person? =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person>(message) {
                errorCounter++
            }

        assertNull(person)
        assertEquals(1, errorCounter)
    }

    @Test
    fun fromSqsMessageOrNullWithErrorHandlerAndMapperWithError() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()
        var errorCounter = 0
        val errorHandler: (Throwable) -> Unit = { errorCounter++ }

        val person =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person, Int>(
                message,
                errorHandler
            ) {
                it.name.length
            }

        assertNull(person)
        assertEquals(1, errorCounter)
    }

    @Test
    fun fromSqsMessageOrNullWithErrorHandlerAndMapperWithoutError() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
                "email": "isfor@thechildren.com"
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()
        var errorCounter = 0
        val errorHandler: (Throwable) -> Unit = { errorCounter++ }

        val nameLength =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person, Int>(
                message,
                errorHandler
            ) {
                it.name.length
            }

        assertEquals(6, nameLength)
        assertEquals(0, errorCounter)
    }

    @Test
    fun fromSqsMessageWithMapperOnly() {
        val body =
            """
            {
                "name": "Wutang",
                "id": 1,
                "email": "isfor@thechildren.com"
            }
        """
                .trimIndent()

        val message = Message.builder().body(body).build()
        val nameLength =
            Person.newBuilder().fromSqsMessageOrNull<Person.Builder, Person, Int>(
                message,
            ) {
                it.name.length
            }

        assertEquals(6, nameLength)
    }
}
