package com.example.medreminder.ui.components

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.medreminder.util.TimeUtils
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.data.remote.DrugApiService
import com.example.medreminder.data.remote.repository.DrugSearchRepository
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.ui.theme.PrimaryBlue
import com.example.medreminder.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class MedicationSuggestionItem(
    val name: String,
    val defaultDosage: String,
    val defaultForm: String = "Tablet",
    val category: String = ""
)

val commonMedicationDatabase = listOf(
    MedicationSuggestionItem("Crizotinib 250mg", "250mg", "Capsule"),
    MedicationSuggestionItem("Gefitinib 250mg", "250mg", "Tablet"),
    MedicationSuggestionItem("Imatinib 400mg", "400mg", "Tablet"),
    MedicationSuggestionItem("Erlotinib 150mg", "150mg", "Tablet"),
    MedicationSuggestionItem("Paracetamol 500mg", "500mg", "Tablet"),
    MedicationSuggestionItem("Paracetamol 650mg", "650mg", "Tablet"),
    MedicationSuggestionItem("Lisinopril 10mg", "10mg", "Tablet"),
    MedicationSuggestionItem("Amlodipine 5mg", "5mg", "Tablet"),
    MedicationSuggestionItem("Metformin 500mg", "500mg", "Tablet"),
    MedicationSuggestionItem("Ibuprofen 400mg", "400mg", "Tablet"),
    MedicationSuggestionItem("Aspirin 75mg", "75mg", "Tablet"),
    MedicationSuggestionItem("Omeprazole 20mg", "20mg", "Capsule"),
    MedicationSuggestionItem("Pantoprazole 40mg", "40mg", "Tablet"),
    MedicationSuggestionItem("Atorvastatin 10mg", "10mg", "Tablet"),
    MedicationSuggestionItem("Rosuvastatin 10mg", "10mg", "Tablet"),
    MedicationSuggestionItem("Cetirizine 10mg", "10mg", "Tablet"),
    MedicationSuggestionItem("Azithromycin 500mg", "500mg", "Tablet"),
    MedicationSuggestionItem("Amoxicillin 500mg", "500mg", "Capsule"),
    MedicationSuggestionItem("Augmentin 625mg", "625mg", "Tablet"),
    MedicationSuggestionItem("Crocin 650mg", "650mg", "Tablet"),
    MedicationSuggestionItem("Dolo 650mg", "650mg", "Tablet"),
    MedicationSuggestionItem("Combiflam", "400mg", "Tablet"),
    MedicationSuggestionItem("Montelukast 10mg", "10mg", "Tablet"),
    MedicationSuggestionItem("Telmisartan 40mg", "40mg", "Tablet"),
    MedicationSuggestionItem("Vitamin D3 60K", "60000IU", "Capsule"),
    MedicationSuggestionItem("Becosules B-Complex", "1 Capsule", "Capsule")
)

/**
 * Bottom Sheet Dialog allowing users to enter full prescription, doctor details, and pharmacy location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationBottomSheet(
    onDismiss: () -> Unit,
    onAddMedication: (Medication) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var pillsPerDose by remember { mutableStateOf("1 Tablet") }
    var form by remember { mutableStateOf("Tablet") }
    var frequency by remember { mutableStateOf("Daily") }
    var timeOfDay by remember { mutableStateOf("Morning") }
    var scheduledTime by remember { mutableStateOf("08:00 AM") }
    var totalPills by remember { mutableStateOf("30") }

    var doctorName by remember { mutableStateOf("") }
    var doctorPhone by remember { mutableStateOf("") }
    var pharmacyName by remember { mutableStateOf("") }
    var pharmacyAddress by remember { mutableStateOf("") }
    var rxNumber by remember { mutableStateOf("") }

    var showLocationPickerModal by remember { mutableStateOf(false) }
    var isSuggestionsExpanded by remember { mutableStateOf(true) }
    var apiSuggestions by remember { mutableStateOf<List<MedicationSuggestionItem>>(emptyList()) }
    var isSearchingApi by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val formOptions = remember {
        listOf("Tablet", "Capsule", "Syrup", "Injection", "Drops", "Inhaler", "Ointment", "Other")
    }
    val shiftOptions = remember {
        listOf(
            "Morning" to "🌅 Morning",
            "Afternoon" to "☀️ Afternoon",
            "Evening" to "🌆 Evening",
            "Night" to "🌙 Night"
        )
    }

    val timePickerDialog = remember(context, scheduledTime) {
        val (h, m) = TimeUtils.getHourAndMinute(scheduledTime)
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = TimeUtils.formatFromHourMinute(hourOfDay, minute)
                scheduledTime = formatted
                timeOfDay = TimeUtils.inferShiftFromHour(hourOfDay)
            },
            h,
            m,
            false // 12-hour AM/PM dial
        )
    }

    // DrugSearchRepository for live global medical database lookup
    val drugRepository = remember {
        val okHttpClient = OkHttpClient.Builder().build()
        val apiService = Retrofit.Builder()
            .baseUrl(DrugApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DrugApiService::class.java)
        DrugSearchRepository(apiService)
    }

    // Coroutine for real-time drug lookup
    LaunchedEffect(name) {
        val query = name.trim()
        if (query.length >= 2) {
            delay(200)
            isSearchingApi = true
            val remoteResults = drugRepository.searchGlobalDrugApi(query)
            apiSuggestions = remoteResults
            isSearchingApi = false
        } else {
            apiSuggestions = emptyList()
            isSearchingApi = false
        }
    }

    // Clean suggestions list without technical labels or chemical dumps
    val combinedSuggestions = remember(name, apiSuggestions) {
        val rawQuery = name.trim()
        val query = rawQuery.lowercase()
        if (query.isBlank()) {
            emptyList()
        } else {
            val typedItem = MedicationSuggestionItem(
                name = rawQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                defaultDosage = "250mg",
                defaultForm = "Tablet"
            )
            val tokens = query.split(" ").filter { it.isNotBlank() }
            val localMatches = commonMedicationDatabase.filter { item ->
                val nameLower = item.name.lowercase()
                tokens.all { token -> nameLower.contains(token) } || nameLower.startsWith(query)
            }
            val list = mutableListOf<MedicationSuggestionItem>()
            list.add(typedItem)
            list.addAll(apiSuggestions)
            list.addAll(localMatches)
            list.distinctBy { it.name.lowercase() }.take(6)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = PrimaryTeal,
            focusedBorderColor = PrimaryTeal,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add New Prescription", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isSuggestionsExpanded = true
                    },
                    label = { Text("Medication Name (e.g. Crizotinib, Paracetamol)") },
                    leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, tint = PrimaryTeal) },
                    trailingIcon = {
                        if (isSearchingApi) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryTeal)
                        } else if (name.isNotBlank()) {
                            IconButton(onClick = { name = ""; isSuggestionsExpanded = false }) {
                                Icon(Icons.Default.Search, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Autocomplete Suggestions Dropdown List
                if (isSuggestionsExpanded && combinedSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal),
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .padding(6.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Select Medicine:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            combinedSuggestions.forEach { suggestion ->
                                Surface(
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = suggestion.name
                                            dosage = suggestion.defaultDosage
                                            form = suggestion.defaultForm
                                            isSuggestionsExpanded = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                suggestion.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${suggestion.defaultForm} • ${suggestion.defaultDosage}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Select",
                                            tint = PrimaryTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Dose Strength & How Many Medicines / Pills Per Dose Input Fields
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dose Strength (250mg)") },
                    placeholder = { Text("e.g. 250mg or 500mg") },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pillsPerDose,
                    onValueChange = { pillsPerDose = it },
                    label = { Text("Pills Per Dose (1 Tablet)") },
                    placeholder = { Text("e.g. 1 Tablet, 2 Pills") },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // 1. Medication Form Selection Chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Form of Medicine",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    formOptions.forEach { opt ->
                        val isSelected = form.equals(opt, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { form = opt },
                            label = {
                                Text(
                                    opt,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTeal,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                if (form.equals("Other", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = form,
                        onValueChange = { form = it },
                        label = { Text("Specify Custom Form") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Total Supply
            OutlinedTextField(
                value = totalPills,
                onValueChange = { totalPills = it },
                label = { Text("Total Bottle Supply (e.g. 30)") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // 2. Shift Selector Chips (Morning, Afternoon, Evening, Night)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Dose Shift",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    shiftOptions.forEach { (shiftKey, shiftLabel) ->
                        val isSelected = timeOfDay.equals(shiftKey, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { timeOfDay = shiftKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = shiftLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. 12-Hour Scheduled Time Picker (Strict 12-hr AM/PM)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Scheduled Time (12-Hour AM/PM)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, PrimaryTeal)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Reminder Time",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = TimeUtils.formatTo12Hour(scheduledTime),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to choose time in 12-hour format",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = PrimaryTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SELECT TIME",
                                color = PrimaryTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Text("Doctor & Prescriber Details (Optional)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name (Optional)") },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = doctorPhone,
                    onValueChange = { doctorPhone = it },
                    label = { Text("Doctor Phone (Optional)") },
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Text("Pharmacy & Rx Location (Optional)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = pharmacyName,
                onValueChange = { pharmacyName = it },
                label = { Text("Pharmacy Name (Optional)") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = pharmacyAddress,
                onValueChange = { pharmacyAddress = it },
                label = { Text("Pharmacy Address / City (Optional)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal) },
                trailingIcon = {
                    IconButton(onClick = { showLocationPickerModal = true }) {
                        Icon(Icons.Default.Place, contentDescription = "Pick Location on Map", tint = PrimaryTeal)
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedButton(
                onClick = { showLocationPickerModal = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("📍 Pick Location on Map / Select Pharmacy", color = PrimaryTeal, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = rxNumber,
                onValueChange = { rxNumber = it },
                label = { Text("Rx Number (Optional)") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val pillsInt = totalPills.toIntOrNull() ?: 30
                        val finalDosage = if (dosage.isNotBlank()) {
                            if (pillsPerDose.isNotBlank() && !pillsPerDose.equals("1 Tablet", ignoreCase = true)) {
                                "${dosage.trim()} ($pillsPerDose per dose)"
                            } else {
                                dosage.trim()
                            }
                        } else {
                            pillsPerDose.ifBlank { "1 Tablet" }
                        }

                        onAddMedication(
                            Medication(
                                name = name.trim(),
                                dosage = finalDosage,
                                form = form.ifBlank { "Tablet" },
                                frequency = frequency,
                                timeOfDay = timeOfDay.ifBlank { TimeUtils.inferShiftFromTime(scheduledTime) },
                                scheduledTime = TimeUtils.formatTo12Hour(scheduledTime),
                                remainingPills = pillsInt,
                                totalPills = pillsInt,
                                accentColorHex = "#009688",
                                prescriberName = doctorName.trim(),
                                prescriberSpecialty = if (doctorName.isNotBlank()) "Specialist Doctor" else "",
                                prescriberPhone = doctorPhone.trim(),
                                pharmacyName = pharmacyName.trim(),
                                pharmacyAddress = pharmacyAddress.trim(),
                                rxNumber = rxNumber.trim(),
                                refillsLeft = 0
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Prescription", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLocationPickerModal) {
        val context = LocalContext.current
        PharmacyLocationPickerModal(
            initialAddress = pharmacyAddress,
            initialName = pharmacyName,
            onDismiss = { showLocationPickerModal = false },
            onLocationSelected = { pickedName, pickedAddress ->
                pharmacyName = pickedName
                pharmacyAddress = pickedAddress
            },
            onOpenExternalMap = { targetQuery ->
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(targetQuery)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(targetQuery)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            }
        )
    }
}
