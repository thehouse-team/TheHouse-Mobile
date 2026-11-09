package com.thehouse.app.data.remote.api

import com.thehouse.app.data.remote.dto.favorites.FavoriteIdsResponseDto
import com.thehouse.app.data.remote.dto.favorites.FavoritesListResponseDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FavoritesApi {

    @GET("api/v1/users/me/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<FavoritesListResponseDto>

    @GET("api/v1/users/me/favorites/ids")
    suspend fun getFavoriteIds(): Response<FavoriteIdsResponseDto>

    @PUT("api/v1/users/me/favorites/{propertyId}")
    suspend fun addFavorite(
        @Path("propertyId") propertyId: String,
    ): Response<Unit>

    @DELETE("api/v1/users/me/favorites/{propertyId}")
    suspend fun removeFavorite(
        @Path("propertyId") propertyId: String,
    ): Response<Unit>
}