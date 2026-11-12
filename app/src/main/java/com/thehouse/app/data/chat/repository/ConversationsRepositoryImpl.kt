package com.thehouse.app.data.chat.repository

import com.thehouse.app.data.remote.api.ChatApi
import com.thehouse.app.data.chat.mapper.toDomain
import com.thehouse.app.data.remote.dto.chat.MarkReadDto
import com.thehouse.app.data.remote.dto.chat.SendMessageDto
import com.thehouse.app.data.remote.dto.visits.ProblemDetailsDto
import com.thehouse.app.domain.error.ChatErrorCode
import com.thehouse.app.domain.error.ChatException
import com.thehouse.app.domain.error.DomainError
import com.thehouse.app.domain.model.Conversation
import com.thehouse.app.domain.model.MessageSent
import com.thehouse.app.domain.model.MessagesFetch
import com.thehouse.app.domain.repository.ConversationRepository
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class ConversationsRepositoryImpl(
    private val api: ChatApi,
    private val json: Json,
) : ConversationRepository {

    override suspend fun getConversations(page: Int, pageSize: Int): Result<List<Conversation>> =
        safeCall {
            val response = api.getConversations(page = page, pageSize = pageSize)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.items.map { it.toDomain() })
            } else {
                Result.failure(errorFor(response))
            }
        }

    override suspend fun getMessages(
        conversationId: String,
        after: String?,
        etag: String?,
    ): Result<MessagesFetch> = safeCall {
        val response = api.getMessages(
            id = conversationId,
            after = after?.takeIf { it.isNotBlank() },
            ifNoneMatch = etag?.takeIf { it.isNotBlank() },
        )
        when {
            // 304 Not Modified: no se modifica la lista; solo informa el poll.
            response.code() == 304 -> Result.success(MessagesFetch.NotModified)
            else -> {
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Result.success(
                        MessagesFetch.Modified(
                            page = body.toDomain(),
                            etag = response.headers()["ETag"],
                        ),
                    )
                } else {
                    Result.failure(errorFor(response))
                }
            }
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        text: String,
        clientMessageId: String,
    ): Result<MessageSent> = safeCall {
        val response = api.sendMessage(
            id = conversationId,
            body = SendMessageDto(text = text, clientMessageId = clientMessageId),
        )
        val body = response.body()
        if (response.isSuccessful && body != null) {
            // 201 = mensaje nuevo; 200 = replay del mismo clientMessageId.
            Result.success(
                MessageSent(
                    message = body.toDomain(),
                    replayed = response.code() == 200,
                ),
            )
        } else {
            Result.failure(errorFor(response))
        }
    }

    override suspend fun markRead(conversationId: String, lastMessageId: String): Result<Boolean> =
        safeCall {
            val response = api.markRead(
                id = conversationId,
                body = MarkReadDto(lastMessageId = lastMessageId),
            )
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(errorFor(response))
            }
        }

    private fun errorFor(response: Response<*>): ChatException {
        val domainError = when (response.code()) {
            401, 403 -> DomainError.Unauthorized
            404 -> DomainError.NotFound
            409 -> DomainError.Conflict
            408 -> DomainError.Timeout
            429 -> DomainError.RateLimited
            in 500..599 -> DomainError.Server
            else -> DomainError.Unknown
        }
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        val code = raw?.let { body ->
            runCatching { json.decodeFromString<ProblemDetailsDto>(body) }.getOrNull()?.code
        }
        return ChatException(domainError, ChatErrorCode.fromApi(code))
    }

    private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (timeout: SocketTimeoutException) {
        Result.failure(ChatException(DomainError.Timeout))
    } catch (network: IOException) {
        Result.failure(ChatException(DomainError.Network))
    } catch (unexpected: Exception) {
        Result.failure(ChatException(DomainError.Unknown))
    }
}
