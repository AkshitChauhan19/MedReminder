package com.example.medreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.medreminder.domain.model.UserAuthState
import com.example.medreminder.ui.components.AppBottomBar
import com.example.medreminder.ui.detail.MedicationDetailScreen
import com.example.medreminder.ui.logs.LogsScreen
import com.example.medreminder.ui.medications.MedicationsScreen
import com.example.medreminder.ui.navigation.Screen
import com.example.medreminder.ui.profile.AuthViewModel
import com.example.medreminder.ui.profile.ProfileScreen
import com.example.medreminder.ui.schedule.ScheduleScreen
import com.example.medreminder.ui.splash.SplashScreen
import com.example.medreminder.ui.theme.MedReminderTheme
import com.example.medreminder.ui.theme.PrimaryBlue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.example.medreminder.ui.theme.PrimaryTeal
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.medreminder.ui.theme.ThemePreferences

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreferences = ThemePreferences.getInstance(this)
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()
            MedReminderTheme(themeMode = themeMode) {
                MainApplicationRoot()
            }
        }
    }
}

@Composable
fun MainApplicationRoot(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else {
        when (val state = authState) {
            is UserAuthState.Authenticated -> {
                MedReminderAppContent(
                    userEmail = state.email ?: "user@gmail.com",
                    displayName = state.displayName ?: state.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User",
                    photoUrl = state.photoUrl
                )
            }
            else -> {
                ProfileScreen(
                    onBackClick = null,
                    viewModel = authViewModel
                )
            }
        }
    }
}

@Composable
fun MedReminderAppContent(
    userEmail: String,
    displayName: String,
    photoUrl: String? = null
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, PrimaryTeal, CircleShape),
                            error = {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(36.dp))
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(userEmail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        label = { Text("Today's Schedule", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Schedule.route)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalPharmacy, contentDescription = null) },
                        label = { Text("My Medications", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Medications.route)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                        label = { Text("Health Logs", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Logs.route)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile & Account", fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Profile.route)
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AppBottomBar(
                    navController = navController,
                    userPhotoUrl = photoUrl
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Schedule.route,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(Screen.Schedule.route) {
                    ScheduleScreen(
                        userName = displayName,
                        userPhotoUrl = photoUrl,
                        onMedicationClick = { medId ->
                            navController.navigate(Screen.Detail.createRoute(medId))
                        },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Medications.route) {
                    MedicationsScreen(
                        userPhotoUrl = photoUrl,
                        onMedicationClick = { medId ->
                            navController.navigate(Screen.Detail.createRoute(medId))
                        },
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Logs.route) {
                    LogsScreen(
                        userPhotoUrl = photoUrl,
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("medId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val medId = backStackEntry.arguments?.getLong("medId") ?: 0L
                    MedicationDetailScreen(
                        medicationId = medId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}