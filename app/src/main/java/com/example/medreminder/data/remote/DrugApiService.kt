package com.example.medreminder.data.remote

import com.example.medreminder.data.remote.model.DrugInfoResponse
import com.example.medreminder.data.remote.model.OpenFdaNdcResponse
import com.example.medreminder.data.remote.model.RxNormResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface DrugApiService {

    // Legacy label search for MedicationRepositoryImpl
    @GET("drug/label.json")
    suspend fun searchDrugLabel(
        @Query("search") query: String,
        @Query("limit") limit: Int = 1
    ): DrugInfoResponse

    // 1. openFDA NDC Drug Products API (Free, Public FDA Database)
    @GET("drug/ndc.json")
    suspend fun searchFdaNdc(
        @Query("search") search: String,
        @Query("limit") limit: Int = 10
    ): OpenFdaNdcResponse

    // 2. NIH RxNorm Drug Search API (Free, Public US National Library of Medicine API)
    @GET
    suspend fun searchRxNormDrugs(
        @Url url: String
    ): RxNormResponse

    companion object {
        const val BASE_URL = "https://api.fda.gov/"
    }
}
