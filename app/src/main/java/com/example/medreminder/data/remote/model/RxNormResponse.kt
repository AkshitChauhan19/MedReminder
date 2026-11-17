package com.example.medreminder.data.remote.model

import com.google.gson.annotations.SerializedName

data class RxNormResponse(
    @SerializedName("drugGroup") val drugGroup: RxNormDrugGroup?,
    @SerializedName("approximateGroup") val approximateGroup: RxNormApproximateGroup?
)

data class RxNormApproximateGroup(
    @SerializedName("inputTerm") val inputTerm: String?,
    @SerializedName("candidate") val candidate: List<RxNormCandidate>?
)

data class RxNormCandidate(
    @SerializedName("rxcui") val rxcui: String?,
    @SerializedName("score") val score: String?,
    @SerializedName("rxConceptProperties") val rxConceptProperties: RxNormConceptProperty?
)

data class RxNormDrugGroup(
    @SerializedName("name") val name: String?,
    @SerializedName("conceptGroup") val conceptGroup: List<RxNormConceptGroup>?
)

data class RxNormConceptGroup(
    @SerializedName("tty") val tty: String?,
    @SerializedName("conceptProperties") val conceptProperties: List<RxNormConceptProperty>?
)

data class RxNormConceptProperty(
    @SerializedName("rxcui") val rxcui: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("synonym") val synonym: String?,
    @SerializedName("tty") val tty: String?
)

data class OpenFdaNdcResponse(
    @SerializedName("results") val results: List<OpenFdaNdcResult>?
)

data class OpenFdaNdcResult(
    @SerializedName("brand_name") val brandName: String?,
    @SerializedName("generic_name") val genericName: String?,
    @SerializedName("dosage_form") val dosageForm: String?,
    @SerializedName("active_ingredients") val activeIngredients: List<OpenFdaIngredient>?
)

data class OpenFdaIngredient(
    @SerializedName("name") val name: String?,
    @SerializedName("strength") val strength: String?
)
