package com.example.medreminder.data.remote.model

import com.google.gson.annotations.SerializedName

data class DrugInfoResponse(
    @SerializedName("results") val results: List<DrugResult>?
)
data class DrugResult(
    @SerializedName("openfda") val openFda : OpenFdaDetails?,
    @SerializedName("purpose") val purpose : List<String>?,
    @SerializedName("warnings") val warnings : List<String>?,
    @SerializedName("dosage_and_administration") val dosageInfo: List<String>?
)
data class OpenFdaDetails(
    @SerializedName("brand_name") val brandName : List<String>,
    @SerializedName("generic_name") val genericName : List<String>,
    @SerializedName("substance_name") val substanceName : List<String>
)
