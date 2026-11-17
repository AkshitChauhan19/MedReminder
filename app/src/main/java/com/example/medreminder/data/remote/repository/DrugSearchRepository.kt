package com.example.medreminder.data.remote.repository

import com.example.medreminder.data.remote.DrugApiService
import com.example.medreminder.ui.components.MedicationSuggestionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrugSearchRepository @Inject constructor(
    private val drugApiService: DrugApiService
) {
    /**
     * Cleans raw chemical strength strings into standard patient dosages (e.g. 250mg, 500mg, 10mg).
     * Filters out bulk chemical units like kg/kg, hp_X, w/w, etc.
     */
    private fun sanitizeDosage(rawStrength: String?): String {
        if (rawStrength.isNullOrBlank()) return "250mg"
        val lower = rawStrength.trim().lowercase()

        // Filter out confusing bulk/chemical units (e.g., "1 kg/kg", "6 [hp_X]/59mL")
        if (lower.contains("kg") || lower.contains("hp_x") || lower.contains("w/w") || lower.contains("v/v") || lower.length > 20) {
            return "250mg"
        }

        val match = Regex("""\b(\d+(\.\d+)?\s*(mg|mcg|g|ml|iu))\b""", RegexOption.IGNORE_CASE).find(lower)
        return match?.value?.replace(" ", "") ?: "250mg"
    }

    /**
     * Capitalizes medicine names cleanly and filters out long chemical dumps.
     */
    private fun cleanMedicineName(rawName: String): String {
        return rawName.trim().lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Searches global drug databases (NIH RxNorm & openFDA) in real-time.
     * Returns clean medicine names, standard dosages, and forms.
     */
    suspend fun searchGlobalDrugApi(query: String): List<MedicationSuggestionItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val results = mutableListOf<MedicationSuggestionItem>()

        // 1. Query NIH RxNorm Approximate Term API
        try {
            val approxUrl = "https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term=${cleanQuery}&maxEntries=20"
            val rxApproxResponse = drugApiService.searchRxNormDrugs(approxUrl)
            rxApproxResponse.approximateGroup?.candidate?.forEach { candidate ->
                val prop = candidate.rxConceptProperties
                val rawName = prop?.name ?: prop?.synonym ?: ""

                // Filter out long chemical lists with multiple commas
                if (rawName.isNotBlank() && rawName.length <= 45 && !rawName.contains(",") &&
                    results.none { it.name.equals(rawName, ignoreCase = true) }) {

                    val form = when {
                        rawName.contains("Tablet", ignoreCase = true) -> "Tablet"
                        rawName.contains("Capsule", ignoreCase = true) -> "Capsule"
                        rawName.contains("Syrup", ignoreCase = true) || rawName.contains("Solution", ignoreCase = true) -> "Syrup"
                        rawName.contains("Injection", ignoreCase = true) -> "Injection"
                        rawName.contains("Inhaler", ignoreCase = true) -> "Inhaler"
                        else -> "Tablet"
                    }

                    results.add(
                        MedicationSuggestionItem(
                            name = cleanMedicineName(rawName),
                            defaultDosage = sanitizeDosage(rawName),
                            defaultForm = form,
                            category = ""
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback silently if offline
        }

        // 2. Query openFDA NDC API with generic_name
        if (results.size < 5) {
            try {
                val searchQuery = "generic_name:$cleanQuery*"
                val fdaResponse = drugApiService.searchFdaNdc(searchQuery, limit = 8)
                fdaResponse.results?.forEach { item ->
                    val name = item.genericName ?: item.brandName ?: ""

                    // Filter out ingredient dumps and comma-separated chemical lists
                    if (name.isNotBlank() && name.length <= 45 && !name.contains(",") &&
                        results.none { it.name.contains(name, ignoreCase = true) }) {

                        val form = item.dosageForm ?: "Tablet"
                        val strength = sanitizeDosage(item.activeIngredients?.firstOrNull()?.strength)

                        results.add(
                            MedicationSuggestionItem(
                                name = cleanMedicineName(name),
                                defaultDosage = strength,
                                defaultForm = form.lowercase().replaceFirstChar { it.uppercase() },
                                category = ""
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback silently
            }
        }

        // 3. Query openFDA NDC API with brand_name
        if (results.size < 5) {
            try {
                val searchQuery = "brand_name:$cleanQuery*"
                val fdaResponse = drugApiService.searchFdaNdc(searchQuery, limit = 8)
                fdaResponse.results?.forEach { item ->
                    val name = item.brandName ?: item.genericName ?: ""

                    if (name.isNotBlank() && name.length <= 45 && !name.contains(",") &&
                        results.none { it.name.contains(name, ignoreCase = true) }) {

                        val form = item.dosageForm ?: "Tablet"
                        val strength = sanitizeDosage(item.activeIngredients?.firstOrNull()?.strength)

                        results.add(
                            MedicationSuggestionItem(
                                name = cleanMedicineName(name),
                                defaultDosage = strength,
                                defaultForm = form.lowercase().replaceFirstChar { it.uppercase() },
                                category = ""
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback silently
            }
        }

        return@withContext results.take(8)
    }
}
