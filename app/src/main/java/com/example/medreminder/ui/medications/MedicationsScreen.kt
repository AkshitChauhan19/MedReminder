package com.example.medreminder.ui.medications

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.util.TimeUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.ui.components.AddMedicationBottomSheet
import com.example.medreminder.ui.components.PharmacyLocationPickerModal
import com.example.medreminder.ui.theme.PrimaryBlue
import com.example.medreminder.ui.theme.PrimaryTeal
import com.example.medreminder.ui.theme.RefillRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    userPhotoUrl: String? = null,
    onMedicationClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: MedicationsViewModel = hiltViewModel()
) {
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }

    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text("Med Reminder", fontWeight = FontWeight.Bold, color = PrimaryTeal)
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = onBgColor)
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, PrimaryTeal, CircleShape),
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = PrimaryTeal)
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = PrimaryTeal)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = PrimaryTeal,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Prescriptions", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("My Medications", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onBgColor)
                    Text("Currently active prescriptions", fontSize = 14.sp, color = onSurfaceVariant)
                }
            }

            if (medications.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No Medications Added",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap the + button below to add your first prescription medication.",
                                fontSize = 14.sp,
                                color = onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(medications) { med ->
                    MedicationCardMockup1(
                        medication = med,
                        onClick = { onMedicationClick(med.id) },
                        onEditClick = { editingMedication = med },
                        onDeleteClick = { medicationToDelete = med }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddSheet) {
        AddMedicationBottomSheet(
            onDismiss = { showAddSheet = false },
            onAddMedication = { newMed -> viewModel.insertMedication(newMed) }
        )
    }

    editingMedication?.let { targetMed ->
        EditPillCountAndMedDialog(
            medication = targetMed,
            onDismiss = { editingMedication = null },
            onSave = { updatedMed ->
                viewModel.insertMedication(updatedMed)
                editingMedication = null
            },
            onDelete = {
                viewModel.deleteMedication(targetMed)
                editingMedication = null
            }
        )
    }

    // Delete Confirmation Dialog
    medicationToDelete?.let { targetMed ->
        AlertDialog(
            onDismissRequest = { medicationToDelete = null },
            title = {
                Text(
                    "Delete Medication",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${targetMed.name}'? This will permanently remove it from your schedule and cloud database.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedication(targetMed)
                        medicationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RefillRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
fun MedicationCardMockup1(
    medication: Medication,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isRefillSoon = medication.remainingPills <= 5
    val accentColor = try {
        Color(android.graphics.Color.parseColor(medication.accentColorHex))
    } catch (_: Exception) {
        PrimaryTeal
    }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val cardBg = if (isRefillSoon) {
        if (isDark) Color(0xFF3B1515) else Color(0xFFFFF5F5)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(if (isRefillSoon) RefillRed else accentColor)
            )

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            medication.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        val cleanForm = if (medication.form.contains(":") || medication.form.contains("PM", true) || medication.form.contains("AM", true)) "Tablet" else medication.form
                        Text(
                            "${medication.dosage} • $cleanForm • ${TimeUtils.formatTo12Hour(medication.scheduledTime)}",
                            fontSize = 14.sp,
                            color = textSecondaryColor
                        )
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = medication.frequency,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (isRefillSoon) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RefillRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REFILL SOON", fontSize = 11.sp, color = RefillRed, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${medication.remainingPills} pills", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RefillRed)
                        } else {
                            Text("REMAINING", fontSize = 11.sp, color = textSecondaryColor, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${medication.remainingPills}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("pills", fontSize = 14.sp, color = textSecondaryColor, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Edit button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onEditClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Prescription",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RefillRed.copy(alpha = 0.15f))
                                .clickable { onDeleteClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Prescription",
                                tint = RefillRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditPillCountAndMedDialog(
    medication: Medication,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(medication.name) }
    var dosage by remember { mutableStateOf(medication.dosage) }
    var form by remember { mutableStateOf(medication.form) }
    var timeOfDay by remember { mutableStateOf(medication.timeOfDay) }
    var scheduledTime by remember { mutableStateOf(medication.scheduledTime) }
    var remainingPillsStr by remember { mutableStateOf(medication.remainingPills.toString()) }
    var totalPillsStr by remember { mutableStateOf(medication.totalPills.toString()) }
    var docName by remember { mutableStateOf(medication.prescriberName ?: "") }
    var docPhone by remember { mutableStateOf(medication.prescriberPhone ?: "") }
    var pharmName by remember { mutableStateOf(medication.pharmacyName ?: "") }
    var pharmAddress by remember { mutableStateOf(medication.pharmacyAddress ?: "") }
    var showPicker by remember { mutableStateOf(false) }

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
        onDismissRequest = onDismiss,
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
                        label = { Text("Total Bottle Supply") },
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
                    label = { Text("Doctor Phone (Optional)") },
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
                    val remPills = remainingPillsStr.toIntOrNull() ?: medication.remainingPills
                    val totPills = totalPillsStr.toIntOrNull() ?: medication.totalPills
                    val updated = medication.copy(
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
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = RefillRed, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
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