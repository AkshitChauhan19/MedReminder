package com.example.medreminder.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.medreminder.domain.model.DayAdherence
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.ui.components.CustomProgressRing
import com.example.medreminder.ui.theme.PrimaryTeal
import com.example.medreminder.ui.theme.RefillRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    userPhotoUrl: String? = null,
    onProfileClick: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val stats by viewModel.adherenceStats.collectAsStateWithLifecycle()
    val logs by viewModel.doseLogs.collectAsStateWithLifecycle()

    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = { Text("Health Logs", fontWeight = FontWeight.Bold, color = PrimaryTeal) },
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
                    Text("Adherence Logs", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = onBgColor)
                    Text("Review your weekly medication statistics and history.", fontSize = 14.sp, color = onSurfaceVariant)
                }
            }

            // Card 1: This Week's Record (Fully Dynamic from Database)
            item {
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
                        Text("This Week's Record", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            stats.weeklyRecord.forEach { day ->
                                CalendarDayBubble(day = day)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryTeal))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("100% Complete", fontSize = 12.sp, color = onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(RefillRed))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Missed Dose", fontSize = 12.sp, color = onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Card 2: Weekly Rate (Fully Dynamic Calculation)
            item {
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Weekly Rate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurfaceColor, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(20.dp))

                        CustomProgressRing(percentage = stats.weeklyPercentage, primaryColor = PrimaryTeal)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${stats.perfectDaysCount} of ${stats.totalDaysCount} days perfect", fontSize = 14.sp, color = onSurfaceVariant)

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Goal: ${stats.goalPercentage}%", fontSize = 12.sp, color = onSurfaceVariant)
                            Text("${stats.weeklyPercentage}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { stats.weeklyPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryTeal,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Section 3: Recent Activity
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onBgColor)
                }
            }

            if (logs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No dose activity logged yet today.", color = onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(logs) { log ->
                    ActivityLogCardMockup2(log = log)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CalendarDayBubble(day: DayAdherence) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            day.dayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (day.isToday) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        when {
            day.isComplete -> {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(PrimaryTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            day.isMissed -> {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(RefillRed.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            day.isToday -> {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, PrimaryTeal, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${day.dayNumber}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                }
            }
            else -> {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${day.dayNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ActivityLogCardMockup2(log: DoseLog) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)
    val cardBg = if (!log.isTaken) {
        if (isDark) Color(0xFF3B1515) else Color(0xFFFFF5F5)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (log.isTaken) PrimaryTeal.copy(alpha = 0.15f) else RefillRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.isTaken) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (log.isTaken) PrimaryTeal else RefillRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.medicationName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${log.dayOfWeek}, ${log.timestamp}",
                    fontSize = 13.sp,
                    color = if (!log.isTaken) RefillRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (log.isTaken) PrimaryTeal.copy(alpha = 0.15f) else RefillRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.isTaken) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (log.isTaken) PrimaryTeal else RefillRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}