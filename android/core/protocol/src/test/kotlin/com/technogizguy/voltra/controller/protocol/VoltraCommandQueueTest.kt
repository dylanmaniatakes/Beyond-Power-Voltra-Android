package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraCommandStatus
import com.technogizguy.voltra.controller.model.VoltraControlCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VoltraCommandQueueTest {
    @Test
    fun sendsOneCommandAtATimeAndReturnsResponse() = runTest {
        val transport = FakeTransport(response = byteArrayOf(0x10, 0x20))
        val queue = VoltraCommandQueue(transport = transport, nowMillis = { 123L })

        val result = queue.send(
            QueuedVoltraCommand(
                id = "load",
                command = VoltraControlCommand.LOAD,
                payload = byteArrayOf(0x01),
                timeoutMillis = 500,
            ),
        )

        assertEquals(VoltraCommandStatus.CONFIRMED, result.status)
        assertEquals("1020", result.rawHex)
        assertEquals(listOf("01"), transport.writes.map { it.toHexString() })
    }

    @Test
    fun timesOutWhenNoResponseArrives() = runTest {
        val queue = VoltraCommandQueue(transport = FakeTransport(response = null), nowMillis = { 456L })

        val result = queue.send(
            QueuedVoltraCommand(
                id = "load",
                command = VoltraControlCommand.LOAD,
                payload = byteArrayOf(0x01),
                timeoutMillis = 10,
                retryCount = 0,
            ),
        )

        assertEquals(VoltraCommandStatus.TIMED_OUT, result.status)
    }

    private class FakeTransport(
        private val response: ByteArray?,
    ) : VoltraCommandTransport {
        val writes = mutableListOf<ByteArray>()

        override suspend fun write(payload: ByteArray) {
            writes += payload
        }

        override suspend fun awaitResponse(commandId: String, timeoutMillis: Long): ByteArray? {
            if (response == null) delay(timeoutMillis + 1)
            return response
        }
    }
}
