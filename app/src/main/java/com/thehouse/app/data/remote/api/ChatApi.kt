package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.chat.ChatMessageDto
import com.thehouse.app.data.remote.dto.chat.ConversationsListResponseDto
import com.thehouse.app.data.remote.dto.chat.MarkReadDto
import com.thehouse.app.data.remote.dto.chat.MessagesResponseDto
import com.thehouse.app.data.remote.dto.chat.SendMessageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @GET("api/v1/conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<ConversationsListResponseDto>

    @GET("api/v1/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int = 50,
        /** Último ETag conocido para respuestas condicionales (304 Not Modified). */
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<MessagesResponseDto>

    @POST("api/v1/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: SendMessageDto,
    ): Response<ChatMessageDto>

    @POST("api/v1/conversations/{id}/read")
    suspend fun markRead(
        @Path("id") id: String,
        @Body body: MarkReadDto,
    ): Response<Unit>
}
