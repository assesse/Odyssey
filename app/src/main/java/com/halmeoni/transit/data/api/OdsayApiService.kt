package com.halmeoni.transit.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OdsayApiService {
    @GET("v1/api/searchPubTransPathT")
    suspend fun searchPubTransPathT(
        @Query("SX") SX: Double,
        @Query("SY") SY: Double,
        @Query("EX") EX: Double,
        @Query("EY") EY: Double,
        @Query("apiKey") apiKey: String,
        @Query("OPT") OPT: Int = 0
    ): Response<OdsayResponse>
}
