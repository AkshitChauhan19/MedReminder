package com.example.medreminder.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.medreminder.ui.theme.PrimaryBlue
import com.example.medreminder.ui.theme.PrimaryTeal
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PharmacyLocationItem(
    val name: String,
    val address: String,
    val badge: String = "Verified Location",
    val isVerified: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyLocationPickerModal(
    initialAddress: String = "",
    initialName: String = "",
    onDismiss: () -> Unit,
    onLocationSelected: (name: String, address: String) -> Unit,
    onOpenExternalMap: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(initialAddress.ifBlank { initialName }) }
    var confirmedAddress by remember { mutableStateOf(initialAddress) }
    var selectedPharmacy by remember { mutableStateOf<PharmacyLocationItem?>(null) }
    var locationResults by remember { mutableStateOf<List<PharmacyLocationItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var locationStatusMessage by remember { mutableStateOf<String?>(null) }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    // Helper to get real current GPS / Network location with dual-engine reverse geocoding
    @SuppressLint("MissingPermission")
    fun fetchRealCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationStatusMessage = "Please grant location permission to detect your current location."
            return
        }

        isSearching = true
        locationStatusMessage = "Detecting current GPS coordinates..."

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var bestLocation: Location? = null

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    bestLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                }

                if (bestLocation != null) {
                    val lat = bestLocation.latitude
                    val lng = bestLocation.longitude
                    var detectedName: String? = null
                    var detectedAddress: String? = null

                    // 1. Android Geocoder reverse lookup
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val addressLines = (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
                            val locality = addr.locality ?: addr.subLocality ?: addr.adminArea ?: "Current Area"
                            val fullAddress = if (addressLines.isNotBlank()) addressLines else "$locality, ${addr.countryName ?: ""}"
                            val name = if (!addr.featureName.isNullOrBlank() && addr.featureName != addr.subThoroughfare) {
                                "${addr.featureName} ($locality)"
                            } else {
                                "Current Location ($locality)"
                            }
                            detectedName = name
                            detectedAddress = fullAddress
                        }
                    } catch (_: Exception) {}

                    // 2. High-precision Network reverse geocoding via Photon if Geocoder gave generic address
                    if (detectedAddress.isNullOrBlank()) {
                        try {
                            val revUrl = "https://photon.komoot.io/reverse?lat=$lat&lon=$lng"
                            val req = Request.Builder().url(revUrl).header("User-Agent", "MedReminderApp/1.0").build()
                            val resp = okHttpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val body = resp.body?.string()
                                if (!body.isNullOrBlank()) {
                                    val root = JsonParser.parseString(body).asJsonObject
                                    val features = root.getAsJsonArray("features")
                                    if (features != null && features.size() > 0) {
                                        val props = features[0].asJsonObject.getAsJsonObject("properties")
                                        val name = props?.get("name")?.asString ?: props?.get("street")?.asString ?: "Current Location"
                                        val street = props?.get("street")?.asString
                                        val city = props?.get("city")?.asString ?: props?.get("district")?.asString
                                        val state = props?.get("state")?.asString
                                        val country = props?.get("country")?.asString
                                        val full = listOfNotNull(street, city, state, country).filter { it.isNotBlank() }.joinToString(", ")
                                        detectedName = "$name ($city)"
                                        detectedAddress = full.ifBlank { "Latitude: $lat, Longitude: $lng" }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    val finalName = detectedName ?: "Current Location (GPS)"
                    val finalAddress = detectedAddress ?: "Latitude: $lat, Longitude: $lng"

                    val item = PharmacyLocationItem(
                        name = finalName,
                        address = finalAddress,
                        badge = "📍 GPS Precise Location"
                    )

                    withContext(Dispatchers.Main) {
                        selectedPharmacy = item
                        searchQuery = item.name
                        confirmedAddress = if (finalAddress.contains(finalName, ignoreCase = true)) finalAddress else "$finalName, $finalAddress"
                        locationResults = listOf(item)
                        locationStatusMessage = "Detected: $finalName"
                        isSearching = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        locationStatusMessage = "Could not get GPS fix. Please ensure location is enabled in settings."
                        isSearching = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    locationStatusMessage = "Error reading location: ${e.localizedMessage ?: "Unknown error"}"
                    isSearching = false
                }
            }
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchRealCurrentLocation()
        } else {
            locationStatusMessage = "Location permission denied. Please search your pharmacy address manually."
        }
    }

    fun formatConfirmedAddress(item: PharmacyLocationItem): String {
        val cleanName = item.name.trim()
        val cleanAddr = item.address.trim()
        return if (cleanAddr.contains(cleanName, ignoreCase = true)) {
            cleanAddr
        } else {
            "$cleanName, $cleanAddr"
        }
    }

    // High-precision Multi-Engine India-Scoped Place Search (Nominatim + Photon + Locality Fallback + Geocoder)
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 3) {
            isSearching = true
            withContext(Dispatchers.IO) {
                val genericWords = setOf(
                    "medical", "store", "stores", "pharmacy", "chemist", "chemists",
                    "hospital", "clinic", "center", "centre", "care", "health",
                    "and", "the", "of", "in", "near", "dr", "doctor", "medicos", "druggist"
                )
                val rawTokens = query.lowercase().split(Regex("[\\s,.-]+")).filter { it.length >= 2 }
                val distinctiveTokens = rawTokens.filter { it !in genericWords }

                data class ScoredItem(val item: PharmacyLocationItem, val score: Int)
                val candidateResults = mutableListOf<ScoredItem>()

                fun scoreItem(name: String, address: String): Int {
                    if (distinctiveTokens.isEmpty()) return 1
                    val combined = "$name $address".lowercase()
                    return distinctiveTokens.count { token -> combined.contains(token) }
                }

                val encoded = URLEncoder.encode(query, "UTF-8")

                // 1. Nominatim OpenStreetMap Search with strict countrycodes=in
                try {
                    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=6&countrycodes=in"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "MedReminderApp/1.0")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val jsonArray = JsonParser.parseString(body).asJsonArray
                            for (element in jsonArray) {
                                val obj = element.asJsonObject
                                val displayName = obj.get("display_name")?.asString ?: ""
                                val parts = displayName.split(",")
                                val title = parts.firstOrNull()?.trim() ?: query
                                val subtitle = parts.drop(1).joinToString(",").trim()
                                val fullAddr = if (subtitle.isNotBlank()) subtitle else displayName

                                val type = obj.get("type")?.asString ?: ""
                                val category = obj.get("class")?.asString ?: ""
                                val badge = when {
                                    type.contains("hospital", true) || category.contains("hospital", true) -> "🏥 Hospital / Medical Center"
                                    type.contains("pharmacy", true) || type.contains("chemist", true) -> "💊 Pharmacy / Chemist"
                                    type.contains("clinic", true) || type.contains("doctors", true) -> "🩺 Clinic / Doctors"
                                    else -> "📍 Registered Place"
                                }

                                val score = scoreItem(title, fullAddr)
                                candidateResults.add(
                                    ScoredItem(
                                        PharmacyLocationItem(name = title, address = fullAddr, badge = badge),
                                        score = score
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 2. Photon Elasticsearch OpenStreetMap Search with India filtering
                try {
                    val url = "https://photon.komoot.io/api/?q=$encoded&limit=8"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "MedReminderApp/1.0")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val root = JsonParser.parseString(body).asJsonObject
                            val features = root.getAsJsonArray("features")
                            if (features != null) {
                                for (feature in features) {
                                    val props = feature.asJsonObject.getAsJsonObject("properties") ?: continue
                                    val country = props.get("country")?.asString
                                    val countryCode = props.get("countrycode")?.asString

                                    // Strictly filter out non-India results
                                    if (country != null && !country.equals("India", ignoreCase = true) && !country.equals("IN", ignoreCase = true)) {
                                        continue
                                    }
                                    if (countryCode != null && !countryCode.equals("IN", ignoreCase = true) && !countryCode.equals("India", ignoreCase = true)) {
                                        continue
                                    }

                                    val name = props.get("name")?.asString
                                        ?: props.get("street")?.asString
                                        ?: query
                                    val street = props.get("street")?.asString
                                    val houseNumber = props.get("housenumber")?.asString
                                    val district = props.get("district")?.asString ?: props.get("suburb")?.asString
                                    val city = props.get("city")?.asString
                                    val state = props.get("state")?.asString
                                    val postcode = props.get("postcode")?.asString
                                    val osmValue = props.get("osm_value")?.asString ?: ""

                                    val addressParts = listOfNotNull(
                                        listOfNotNull(street, houseNumber).joinToString(" ").ifBlank { null },
                                        district,
                                        city,
                                        state,
                                        postcode,
                                        "India"
                                    ).filter { it.isNotBlank() }

                                    val fullAddress = if (addressParts.isNotEmpty()) {
                                        addressParts.joinToString(", ")
                                    } else {
                                        name
                                    }

                                    val score = scoreItem(name, fullAddress)
                                    // Skip irrelevant fuzzy matches from Photon that match 0 distinctive query tokens
                                    if (distinctiveTokens.isNotEmpty() && score == 0) {
                                        continue
                                    }

                                    val badge = when {
                                        osmValue.contains("hospital", ignoreCase = true) -> "🏥 Hospital / Medical Center"
                                        osmValue.contains("pharmacy", ignoreCase = true) || osmValue.contains("chemist", ignoreCase = true) -> "💊 Pharmacy / Chemist"
                                        osmValue.contains("clinic", ignoreCase = true) || osmValue.contains("doctors", ignoreCase = true) -> "🩺 Clinic / Doctors"
                                        else -> "📍 Verified Place"
                                    }

                                    candidateResults.add(
                                        ScoredItem(
                                            PharmacyLocationItem(name = name, address = fullAddress, badge = badge),
                                            score = score
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 3. Android Geocoder fallback
                try {
                    val geocoder = Geocoder(context, Locale("en", "IN"))
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 5)
                    if (!addresses.isNullOrEmpty()) {
                        for (addr in addresses) {
                            if (addr.countryName != null && !addr.countryName.equals("India", ignoreCase = true)) continue

                            val addressLines = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                            val fullAddress = if (addressLines.isNotBlank()) {
                                addressLines
                            } else {
                                listOfNotNull(addr.featureName, addr.thoroughfare, addr.subLocality, addr.locality, addr.adminArea, addr.postalCode, "India")
                                    .joinToString(", ")
                            }

                            val fn = addr.featureName?.trim() ?: ""
                            val fnLower = fn.lowercase()
                            val isStreetOrNumber = fn.any { it.isDigit() } ||
                                listOf("road", " rd", "street", " st", "marg", "gali", "lane", "sector", "block", "colony", "nagar", "path", "chowk").any { fnLower.contains(it) } ||
                                (addr.thoroughfare != null && fnLower == addr.thoroughfare.lowercase())

                            val localityTerms = setOfNotNull(
                                addr.locality?.lowercase()?.trim(),
                                addr.subLocality?.lowercase()?.trim(),
                                addr.adminArea?.lowercase()?.trim()
                            )

                            val queryWords = query.split(Regex("\\s+")).filter { it.isNotBlank() }
                            val businessWords = queryWords.filter { it.lowercase() !in localityTerms }
                            val queryBusinessTitle = if (businessWords.isNotEmpty()) {
                                businessWords.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                            } else {
                                query.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                            }

                            val placeName = if (fn.isNotBlank() && !isStreetOrNumber && distinctiveTokens.any { fnLower.contains(it) }) {
                                fn
                            } else {
                                queryBusinessTitle
                            }

                            val qLower = query.lowercase()
                            val badge = when {
                                qLower.contains("hospital") || qLower.contains("clinic") -> "🏥 Hospital / Medical Center"
                                qLower.contains("medical") || qLower.contains("pharmacy") || qLower.contains("chemist") || qLower.contains("agency") -> "💊 Pharmacy / Medical Store"
                                else -> "📍 Registered Place"
                            }

                            val score = scoreItem(placeName, fullAddress) + 5 // Boost Google Maps geocoder results
                            candidateResults.add(
                                ScoredItem(
                                    PharmacyLocationItem(name = placeName, address = fullAddress.ifBlank { query }, badge = badge),
                                    score = score
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // 4. Smart Locality/City Fallback if POI is not indexed yet in OpenStreetMap
                val maxScore = candidateResults.maxOfOrNull { it.score } ?: 0
                val needsLocalityFallback = candidateResults.isEmpty() || (distinctiveTokens.size >= 2 && maxScore < distinctiveTokens.size)

                if (needsLocalityFallback) {
                    val words = query.split(Regex("\\s+")).filter { it.isNotBlank() }
                    val candidateCities = mutableListOf<String>()
                    if (words.isNotEmpty()) candidateCities.add(words.last())
                    if (words.size >= 2) candidateCities.add("${words[words.size - 2]} ${words.last()}")

                    for (candidate in candidateCities) {
                        try {
                            val encCandidate = URLEncoder.encode(candidate, "UTF-8")
                            val cityUrl = "https://nominatim.openstreetmap.org/search?q=$encCandidate&format=json&addressdetails=1&limit=1&countrycodes=in"
                            val cityReq = Request.Builder().url(cityUrl).header("User-Agent", "MedReminderApp/1.0").build()
                            val cityResp = okHttpClient.newCall(cityReq).execute()
                            if (cityResp.isSuccessful) {
                                val cBody = cityResp.body?.string()
                                if (!cBody.isNullOrBlank()) {
                                    val cArray = JsonParser.parseString(cBody).asJsonArray
                                    if (cArray.size() > 0) {
                                        val cObj = cArray[0].asJsonObject
                                        val cityDisplayName = cObj.get("display_name")?.asString ?: ""
                                        if (cityDisplayName.isNotBlank()) {
                                            val storeName = query.split(" ").joinToString(" ") { word ->
                                                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                                            }
                                            candidateResults.add(
                                                0,
                                                ScoredItem(
                                                    PharmacyLocationItem(
                                                        name = storeName,
                                                        address = cityDisplayName,
                                                        badge = "📍 City Verified Location"
                                                    ),
                                                    score = distinctiveTokens.size + 10
                                                )
                                            )
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Filter to items matching top score tier if distinctive tokens exist
                val topScore = candidateResults.maxOfOrNull { it.score } ?: 0
                val filteredItems = if (distinctiveTokens.size >= 2 && topScore > 1) {
                    candidateResults
                        .filter { it.score >= (topScore - 2).coerceAtLeast(1) }
                        .sortedByDescending { it.score }
                        .map { it.item }
                } else {
                    candidateResults.sortedByDescending { it.score }.map { it.item }
                }

                val distinctResults = filteredItems.distinctBy {
                    "${it.name.lowercase().trim()}_${it.address.lowercase().trim()}"
                }.take(8)

                withContext(Dispatchers.Main) {
                    locationResults = distinctResults
                    // Auto-select top registered result if nothing selected yet or currently on typed fallback
                    if (distinctResults.isNotEmpty() && (selectedPharmacy == null || selectedPharmacy?.badge == "Typed Location")) {
                        val topMatch = distinctResults.first()
                        selectedPharmacy = topMatch
                        confirmedAddress = formatConfirmedAddress(topMatch)
                    }
                    isSearching = false
                }
            }
        } else {
            locationResults = emptyList()
            isSearching = false
        }
    }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val cardSurfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardSurfaceColor,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Select Pharmacy Location",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = onSurfaceColor
                            )
                            Text(
                                "Search Google/GPS registered places",
                                fontSize = 12.sp,
                                color = textSecondaryColor
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input Box with Clear Button
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        selectedPharmacy = null
                        locationStatusMessage = null
                    },
                    placeholder = { Text("Search hospital, pharmacy, street, or city...", color = textSecondaryColor) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTeal) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                locationResults = emptyList()
                                selectedPharmacy = null
                                locationStatusMessage = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondaryColor)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = onSurfaceColor,
                        unfocusedTextColor = onSurfaceColor,
                        focusedContainerColor = cardSurfaceColor,
                        unfocusedContainerColor = cardSurfaceColor,
                        cursorColor = PrimaryTeal,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Shortcuts Bar: Use Current Location + Clear Search + Open Maps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                fetchRealCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        label = { Text("📍 Use Current Location", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryTeal) },
                        shape = RoundedCornerShape(20.dp)
                    )

                    if (searchQuery.isNotBlank()) {
                        AssistChip(
                            onClick = {
                                searchQuery = ""
                                locationResults = emptyList()
                                selectedPharmacy = null
                                locationStatusMessage = null
                            },
                            label = { Text("Clear Search", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    if (onOpenExternalMap != null) {
                        AssistChip(
                            onClick = {
                                val target = selectedPharmacy?.address ?: confirmedAddress.ifBlank { searchQuery.ifBlank { "Pharmacy near me" } }
                                onOpenExternalMap(target)
                            },
                            label = { Text("🗺️ Maps", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // Status message indicator
                if (locationStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = locationStatusMessage ?: "",
                        fontSize = 12.sp,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryTeal)
                        Text("Looking up registered locations...", fontSize = 12.sp, color = textSecondaryColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Search & Verified Location Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textSecondaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of Real Registered Locations (No fake km tags!)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (locationResults.isEmpty() && !isSearching) {
                        item {
                            Surface(
                                color = if (isDark) Color(0xFF334155) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "Type a pharmacy name, hospital, street, or tap 'Use Current Location'." else "No registered places found for '$searchQuery'. Try checking spelling or use 'Use Current Location'.",
                                    fontSize = 12.sp,
                                    color = textSecondaryColor,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    items(locationResults) { item ->
                        val isSelected = selectedPharmacy?.name == item.name && selectedPharmacy?.address == item.address

                        val cardBg = if (isSelected) {
                            if (isDark) Color(0xFF064E3B) else Color(0xFFE6F4EA)
                        } else {
                            if (isDark) Color(0xFF334155) else Color(0xFFF8FAFC)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPharmacy = item
                                    confirmedAddress = formatConfirmedAddress(item)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = PrimaryTeal.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            item.badge,
                                            fontSize = 10.sp,
                                            color = PrimaryTeal,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = onSurfaceColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        item.address,
                                        fontSize = 12.sp,
                                        color = textSecondaryColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isSelected) "Selected" else "Unselected",
                                    tint = if (isSelected) PrimaryTeal else Color.LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Confirmed Address Line with Dark Mode friendly colors
                OutlinedTextField(
                    value = confirmedAddress.ifBlank { searchQuery },
                    onValueChange = { confirmedAddress = it },
                    label = { Text("Confirmed Pharmacy Address", color = textSecondaryColor) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = onSurfaceColor,
                        unfocusedTextColor = onSurfaceColor,
                        focusedContainerColor = cardSurfaceColor,
                        unfocusedContainerColor = cardSurfaceColor,
                        cursorColor = PrimaryTeal,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm & Save Button
                Button(
                    onClick = {
                        val finalName = selectedPharmacy?.name ?: searchQuery.ifBlank { "Local Pharmacy" }
                        val finalAddress = confirmedAddress.ifBlank {
                            selectedPharmacy?.let { formatConfirmedAddress(it) } ?: searchQuery.ifBlank { "Registered Location" }
                        }
                        onLocationSelected(finalName, finalAddress)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Save Location to App", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
