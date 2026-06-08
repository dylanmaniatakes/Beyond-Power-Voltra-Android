package com.technogizguy.voltra.controller.protocol

import com.technogizguy.voltra.controller.model.VoltraCommandResult
import com.technogizguy.voltra.controller.model.VoltraCommandStatus
import com.technogizguy.voltra.controller.model.VoltraControlCommand
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

data class QueuedVoltraCommand(
    val id: String,
    val command: VoltraControlCommand,
    val payload: ByteArray,
    val timeoutMillis: Long = 2_000L,
    val retryCount: Int = 1,
)

interface VoltraCommandTransport {
    suspend fun write(payload: ByteArray)

    suspend fun awaitResponse(commandId: String, timeoutMillis: Long): ByteArray?
}

class VoltraCommandQueue(
    private val transport: VoltraCommandTransport,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    suspend fun send(command: QueuedVoltraCommand): VoltraCommandResult = mutex.withLock {
        var lastError: Throwable? = null
        repeat(command.retryCount + 1) { attempt ->
            try {
                transport.write(command.payload)
                val response = withTimeoutOrNull(command.timeoutMillis) {
                    transport.awaitResponse(command.id, command.timeoutMillis)
                }
                if (response != null) {
                    return@withLock VoltraCommandResult(
                        command = command.command,
                        status = VoltraCommandStatus.CONFIRMED,
                        message = "Command confirmed on attempt ${attempt + 1}.",
                        timestampMillis = nowMillis(),
                        rawHex = response.toHexString(),
                    )
                }
            } catch (error: Throwable) {
                lastError = error
            }
        }

        VoltraCommandResult(
            command = command.command,
            status = if (lastError == null) VoltraCommandStatus.TIMED_OUT else VoltraCommandStatus.FAILED,
            message = lastError?.message ?: "Timed out waiting for a matching VOLTRA response.",
            timestampMillis = nowMillis(),
            rawHex = command.payload.toHexString(),
        )
    }
}
