package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.notifications.NotificationsListResponseDto
import com.thehouse.app.data.remote.dto.notifications.ReadAllNotificationsDto
import com.thehouse.app.data.remote.dto.notifications.UnreadCountResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApi {

    @GET("api/v1/users/me/notifications")
    suspend fun getNotifications(
        @Query("unread") unread: Boolean? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<NotificationsListResponseDto>

    @GET("api/v1/users/me/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("If-None-Match") ifNoneMatch: String? = null,
    ): Response<UnreadCountResponseDto>

    @POST("api/v1/users/me/notifications/{id}/read")
    suspend fun markRead(
        @Path("id") id: String,
    ): Response<Unit>

    @POST("api/v1/users/me/notifications/read-all")
    suspend fun markAllRead(
        @Body body: ReadAllNotificationsDto = ReadAllNotificationsDto(),
    ): Response<Unit>
}
