package com.example.medreminder.ui.detail

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import com.example.medreminder.util.TimeUtils
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.ui.components.PharmacyLocationPickerModal
import com.example.medreminder.ui.theme.PrimaryBlue
import com.example.medreminder.ui.theme.PrimaryTeal
import com.example.medreminder.ui.theme.RefillRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MedicationDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val medication by viewModel.medication.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Prescription Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (medication != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RefillRed)
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
    ) { innerPadding ->
        if (medication == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryTeal)
            }
        } else {
            val med = medication!!
            val parseColor = remember(med.accentColorHex) {
                try {
                    Color(android.graphics.Color.parseColor(med.accentColorHex))
                } catch (_: Exception) {
                    PrimaryTeal
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(bgColor)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Main Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(parseColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocalPharmacy,
                                        contentDescription = null,
                                        tint = parseColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(med.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    Text(med.form, fontSize = 14.sp, color = onSurfaceVariant)
                                }
                            }

                            Surface(
                                color = if (med.remainingPills <= 5) RefillRed.copy(alpha = 0.15f) else PrimaryTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${med.remainingPills} / ${med.totalPills} left",
                                    fontSize = 12.sp,
                                    color = if (med.remainingPills <= 5) RefillRed else PrimaryTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SCHEDULE", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text("${TimeUtils.formatTo12Hour(med.scheduledTime)} (${TimeUtils.getShiftForMedication(med)})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                            }

                            Button(
                                onClick = { viewModel.refillMedication(med) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refill", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Card 2: Instructions & Dosage Quantity
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instructions & Dosage", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                        }

                        Column {
                            Text("DOSAGE & PILLS PER DOSE", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text("${med.dosage}, ${med.frequency}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                        }

                        med.specialInstructions?.let { instructions ->
                            Column {
                                Text("SPECIAL INSTRUCTIONS", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(instructions, fontSize = 14.sp, color = onSurfaceColor)
                            }
                        }

                        Surface(
                            color = RefillRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RefillRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RefillRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Finish the entire prescribed course, even if you feel better.",
                                    fontSize = 13.sp,
                                    color = RefillRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Card 3: Prescriber Section
                val hasDoctor = !med.prescriberName.isNullOrBlank() || !med.prescriberPhone.isNullOrBlank()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Prescriber", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (hasDoctor) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(med.prescriberName?.ifBlank { "Doctor / Prescriber" } ?: "Doctor / Prescriber", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                    if (!med.prescriberSpecialty.isNullOrBlank()) {
                                        Text(med.prescriberSpecialty, fontSize = 13.sp, color = onSurfaceVariant)
                                    }
                                    if (!med.prescriberPhone.isNullOrBlank()) {
                                        Text("Tel: ${med.prescriberPhone}", fontSize = 12.sp, color = PrimaryTeal)
                                    }
                                }
                            }
                            if (!med.prescriberPhone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${med.prescriberPhone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Contact Doctor", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                }
                            }
                        } else {
                            Text("No Doctor Details Added", fontSize = 14.sp, color = onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Doctor Info", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            }
                        }
                    }
                }

                // Card 4: Pharmacy Section
                val hasPharmacy = !med.pharmacyName.isNullOrBlank() || !med.pharmacyAddress.isNullOrBlank()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("Pharmacy", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (hasPharmacy) {
                            if (!med.pharmacyName.isNullOrBlank()) {
                                Text(med.pharmacyName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                            }
                            if (!med.pharmacyAddress.isNullOrBlank()) {
                                Text(med.pharmacyAddress, fontSize = 13.sp, color = onSurfaceVariant)
                            }

                            if (!med.rxNumber.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Rx #: ${med.rxNumber}", fontSize = 12.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                        Text("Refills left: ${med.refillsLeft}", fontSize = 12.sp, color = onSurfaceVariant)
                                    }
                                }
                            }

                            if (!med.pharmacyAddress.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        val targetAddress = Uri.encode(med.pharmacyAddress)
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$targetAddress"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Get Directions", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                }
                            }
                        } else {
                            Text("No Pharmacy Location Added", fontSize = 14.sp, color = onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Pharmacy Location", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prominent Delete Medication Action Button
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RefillRed),
                    border = BorderStroke(1.dp, RefillRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RefillRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Medication", color = RefillRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && medication != null) {
        val med = medication!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    "Delete Medication",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${med.name}'? This will permanently remove it from your schedule and your Firebase cloud database.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedication(med)
                        showDeleteConfirmation = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RefillRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (showEditDialog && medication != null) {
        var name by remember { mutableStateOf(medication!!.name) }
        var dosage by remember { mutableStateOf(medication!!.dosage) }
        var form by remember { mutableStateOf(medication!!.form) }
        var timeOfDay by remember { mutableStateOf(medication!!.timeOfDay) }
        var scheduledTime by remember { mutableStateOf(medication!!.scheduledTime) }
        var remainingPillsStr by remember { mutableStateOf(medication!!.remainingPills.toString()) }
        var totalPillsStr by remember { mutableStateOf(medication!!.totalPills.toString()) }
        var docName by remember { mutableStateOf(medication!!.prescriberName ?: "") }
        var docPhone by remember { mutableStateOf(medication!!.prescriberPhone ?: "") }
        var pharmName by remember { mutableStateOf(medication!!.pharmacyName ?: "") }
        var pharmAddress by remember { mutableStateOf(medication!!.pharmacyAddress ?: "") }
        var showPicker by remember { mutableStateOf(false) }

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

        val (h, m) = remember(scheduledTime) { TimeUtils.getHourAndMinute(scheduledTime) }
        val timePickerDialog = remember(context, scheduledTime) {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val formatted = TimeUtils.formatFromHourMinute(hourOfDay, minute)
                    scheduledTime = formatted
                    timeOfDay = TimeUtils.inferShiftFromHour(hourOfDay)
                },
                h,
                m,
                false
            )
        }

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = PrimaryTeal,
            focusedBorderColor = PrimaryTeal,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Prescription & Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medication Name") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage / Pills per Dose") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Form Chips
                    Text("Form", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formOptions.forEach { opt ->
                            val isSelected = form.equals(opt, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { form = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Shift Chips
                    Text("Shift", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = shiftLabel,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // 12-Hour Scheduled Time
                    Text("Reminder Time (12-Hour)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { timePickerDialog.show() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, PrimaryTeal),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TimeUtils.formatTo12Hour(scheduledTime),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text("PICK TIME", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = remainingPillsStr,
                            onValueChange = { remainingPillsStr = it },
                            label = { Text("Remaining Pills") },
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalPillsStr,
                            onValueChange = { totalPillsStr = it },
                            label = { Text("Total Supply") },
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text("Doctor Name (Optional)") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docPhone,
                        onValueChange = { docPhone = it },
                        label = { Text("Doctor Phone Number (Optional)") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pharmName,
                        onValueChange = { pharmName = it },
                        label = { Text("Pharmacy Name (Optional)") },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pharmAddress,
                        onValueChange = { pharmAddress = it },
                        label = { Text("Pharmacy Address (Optional)") },
                        colors = textFieldColors,
                        trailingIcon = {
                            IconButton(onClick = { showPicker = true }) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Pick Location", tint = PrimaryTeal)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val remPills = remainingPillsStr.toIntOrNull() ?: medication!!.remainingPills
                        val totPills = totalPillsStr.toIntOrNull() ?: medication!!.totalPills
                        val updated = medication!!.copy(
                            name = name.trim(),
                            dosage = dosage.trim(),
                            form = form.ifBlank { "Tablet" },
                            timeOfDay = timeOfDay.ifBlank { TimeUtils.inferShiftFromTime(scheduledTime) },
                            scheduledTime = TimeUtils.formatTo12Hour(scheduledTime),
                            remainingPills = remPills,
                            totalPills = totPills,
                            prescriberName = docName.trim(),
                            prescriberPhone = docPhone.trim(),
                            pharmacyName = pharmName.trim(),
                            pharmacyAddress = pharmAddress.trim()
                        )
                        viewModel.updateMedication(updated)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showEditDialog = false
                        showDeleteConfirmation = true
                    }) {
                        Text("Delete", color = RefillRed, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        )

        if (showPicker) {
            PharmacyLocationPickerModal(
                initialAddress = pharmAddress,
                initialName = pharmName,
                onDismiss = { showPicker = false },
                onLocationSelected = { pickedName, pickedAddress ->
                    pharmName = pickedName
                    pharmAddress = pickedAddress
                }
            )
        }
    }
}