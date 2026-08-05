package com.kaganim.fairyai.data.remote

import retrofit2.http.GET

interface ApiService {
    @GET("example")
    suspend fun getExampleData(): String
}
