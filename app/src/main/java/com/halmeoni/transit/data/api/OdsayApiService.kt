package com.halmeoni.transit.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OdsayApiService {
    @GET("v1/api/searchPubTransPathT")
    suspend fun searchPubTransPathT(
        @Query("apiKey") apiKey: String,
        @Query("SX") SX: Double,
        @Query("SY") SY: Double,
        @Query("EX") EX: Double,
        @Query("EY") EY: Double,
        @Query("lang") lang: Int = 0,
        @Query("output") output: String = "json",
        @Query("OPT") OPT: Int = 0,
        @Query("SearchType") SearchType: Int = 0,
        @Query("SearchPathType") SearchPathType: Int = 0
    ): Response<OdsayResponse>
}
