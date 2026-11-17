package com.example.medreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.SubcomposeAsyncImage
import com.example.medreminder.ui.navigation.Screen
import com.example.medreminder.ui.theme.PrimaryTeal

/**
 * Custom Bottom Navigation Bar matching design mockups with theme-adaptive colors and Google avatar support.
 */
@Composable
fun AppBottomBar(
    navController: NavController,
    userPhotoUrl: String? = null
) {
    val items = listOf(
        Screen.Schedule to Icons.Default.CalendarToday,
        Screen.Medications to Icons.Default.LocalPharmacy,
        Screen.Logs to Icons.Default.ShowChart,
        Screen.Profile to Icons.Default.Person
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = surfaceColor,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, icon) ->
                val isSelected = currentRoute == screen.route

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Schedule.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) PrimaryTeal else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (screen == Screen.Profile && !userPhotoUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = userPhotoUrl,
                                contentDescription = screen.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else onSurfaceVariantColor,
                                        shape = CircleShape
                                    ),
                                error = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) Color.White else onSurfaceVariantColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) Color.White else onSurfaceVariantColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PrimaryTeal else onSurfaceVariantColor
                    )
                }
            }
        }
    }
}
