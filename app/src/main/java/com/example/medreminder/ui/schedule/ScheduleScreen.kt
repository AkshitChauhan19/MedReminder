package com.example.medreminder.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medreminder.domain.model.Medication
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.medreminder.ui.components.AddMedicationBottomSheet
import com.example.medreminder.ui.theme.PrimaryBlue
import com.example.medreminder.ui.theme.PrimaryTeal
import com.example.medreminder.ui.theme.RefillRed
import com.example.medreminder.ui.theme.SoftBackground
import com.example.medreminder.util.TimeUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    userName: String = "User",
    userPhotoUrl: String? = null,
    onMedicationClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val medications by viewModel.scheduleMeds.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    val takenCount = medications.count { it.isTakenToday }
    val totalCount = medications.size.coerceAtLeast(1)
    val allCompleted = medications.isNotEmpty() && takenCount == medications.size

    // Ticker state for real-time countdown calculation
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val countdownText = remember(medications, currentTimeMillis) {
        calculateNextDoseCountdown(medications)
    }

    val cleanName = remember(userName) {
        userName.ifBlank { "User" }
    }

    val greetingText = remember(cleanName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning, $cleanName ☀️"
            in 12..16 -> "Good Afternoon, $cleanName 🌤️"
            in 17..21 -> "Good Evening, $cleanName 🌙"
            else -> "Good Night, $cleanName 😴"
        }
    }

    val currentDateText = remember {
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        formatter.format(Date())
    }

    val currentTimeText = remember(currentTimeMillis) {
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        timeFormatter.format(Date())
    }

    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
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
                Icon(Icons.Default.Add, contentDescription = "Add Dose", tint = Color.White)
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
                    Text("TODAY'S SCHEDULE", fontSize = 12.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(greetingText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = onBgColor)
                    Text("$currentDateText • $currentTimeText", fontSize = 13.sp, color = PrimaryTeal, fontWeight = FontWeight.Medium)
                }
            }

            // Dynamic Dose Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (allCompleted) PrimaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (allCompleted) Icons.Default.CheckCircle else Icons.Default.Timer,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            if (allCompleted) {
                                Text("DAILY DOSE STATUS", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text("All Doses Completed!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                Text("Great job keeping up with your meds today.", fontSize = 12.sp, color = onSurfaceVariant)
                            } else {
                                Text("NEXT DOSE IN", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(countdownText ?: "00:00:00", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                            }
                        }
                    }
                }
            }

            // Daily Progress Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("You've taken $takenCount of $totalCount medications today.", fontSize = 13.sp, color = onSurfaceVariant)
                        }

                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { takenCount.toFloat() / totalCount.toFloat() },
                                modifier = Modifier.fillMaxSize(),
                                color = PrimaryTeal,
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text("$takenCount/$totalCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        }
                    }
                }
            }

            val morningMeds = medications.filter { TimeUtils.getShiftForMedication(it) == "Morning" }
            val afternoonMeds = medications.filter { TimeUtils.getShiftForMedication(it) == "Afternoon" }
            val eveningMeds = medications.filter { TimeUtils.getShiftForMedication(it) == "Evening" }
            val nightMeds = medications.filter { TimeUtils.getShiftForMedication(it) == "Night" }
            val otherMeds = medications.filter {
                val shift = TimeUtils.getShiftForMedication(it)
                shift !in listOf("Morning", "Afternoon", "Evening", "Night")
            }

            if (morningMeds.isNotEmpty()) {
                item { TimelineHeader("Morning", Color(0xFF0D9488)) }
                items(morningMeds) { med ->
                    ScheduleItemCard(medication = med, onToggle = { viewModel.toggleDoseTaken(med.id, !med.isTakenToday) }, onClick = { onMedicationClick(med.id) })
                }
            }

            if (afternoonMeds.isNotEmpty()) {
                item { TimelineHeader("Afternoon", PrimaryBlue) }
                items(afternoonMeds) { med ->
                    ScheduleItemCard(medication = med, onToggle = { viewModel.toggleDoseTaken(med.id, !med.isTakenToday) }, onClick = { onMedicationClick(med.id) })
                }
            }

            if (eveningMeds.isNotEmpty()) {
                item { TimelineHeader("Evening", Color(0xFFE65100)) }
                items(eveningMeds) { med ->
                    ScheduleItemCard(medication = med, onToggle = { viewModel.toggleDoseTaken(med.id, !med.isTakenToday) }, onClick = { onMedicationClick(med.id) })
                }
            }

            if (nightMeds.isNotEmpty()) {
                item { TimelineHeader("Night", Color(0xFF6366F1)) }
                items(nightMeds) { med ->
                    ScheduleItemCard(medication = med, onToggle = { viewModel.toggleDoseTaken(med.id, !med.isTakenToday) }, onClick = { onMedicationClick(med.id) })
                }
            }

            if (otherMeds.isNotEmpty()) {
                item { TimelineHeader("Other / As Needed", Color(0xFF64748B)) }
                items(otherMeds) { med ->
                    ScheduleItemCard(medication = med, onToggle = { viewModel.toggleDoseTaken(med.id, !med.isTakenToday) }, onClick = { onMedicationClick(med.id) })
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
}

private fun calculateNextDoseCountdown(meds: List<Medication>): String? {
    val untakenMeds = meds.filter { !it.isTakenToday }
    if (untakenMeds.isEmpty()) return null

    val now = Calendar.getInstance()
    var minDiffMillis = Long.MAX_VALUE

    for (med in untakenMeds) {
        try {
            val (hour, minute) = TimeUtils.getHourAndMinute(med.scheduledTime)
            val doseCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (doseCal.before(now)) {
                doseCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            val diff = doseCal.timeInMillis - now.timeInMillis
            if (diff in 1..<minDiffMillis) {
                minDiffMillis = diff
            }
        } catch (_: Exception) {}
    }

    if (minDiffMillis == Long.MAX_VALUE) return "00:00:00"

    val hours = minDiffMillis / (1000 * 60 * 60)
    val minutes = (minDiffMillis % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = (minDiffMillis % (1000 * 60)) / 1000

    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
fun TimelineHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ScheduleItemCard(
    medication: Medication,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(medication.accentColorHex))
    } catch (_: Exception) {
        PrimaryTeal
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = onToggle) {
                if (medication.isTakenToday) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Taken", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceColor)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = medication.dosage,
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (medication.isTakenToday && !medication.lastTakenTime.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Taken at ${medication.lastTakenTime}",
                            fontSize = 12.sp,
                            color = onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = TimeUtils.formatTo12Hour(medication.scheduledTime),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
        }
    }
}
