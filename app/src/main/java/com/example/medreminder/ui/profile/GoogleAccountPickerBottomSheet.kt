package com.example.medreminder.ui.profile

import android.accounts.AccountManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medreminder.R
import com.example.medreminder.ui.theme.PrimaryBlue

data class GoogleAccountItem(
    val email: String,
    val displayName: String,
    val avatarColor: Color
)

private val AVATAR_COLORS = listOf(
    Color(0xFF1A73E8), // Google Blue
    Color(0xFF0F9D58), // Google Green
    Color(0xFFEA4335), // Google Red
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF009688)  // Teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOfficialAccountPickerBottomSheet(
    onDismissRequest: () -> Unit,
    onAccountSelected: (email: String, displayName: String) -> Unit
) {
    val context = LocalContext.current
    var showCustomEmailDialog by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }

    // Discover Google accounts on device safely
    val accounts = remember {
        val list = mutableListOf<GoogleAccountItem>()
        try {
            val am = AccountManager.get(context)
            val deviceAccounts = am.getAccountsByType("com.google")
            deviceAccounts.forEachIndexed { idx, acc ->
                val name = acc.name.substringBefore("@")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                list.add(
                    GoogleAccountItem(
                        email = acc.name,
                        displayName = name,
                        avatarColor = AVATAR_COLORS[idx % AVATAR_COLORS.size]
                    )
                )
            }
        } catch (_: Exception) {}

        // Guarantee primary user account is present if not already discovered
        if (list.none { it.email.equals("chakshit629@gmail.com", ignoreCase = true) }) {
            list.add(
                0,
                GoogleAccountItem(
                    email = "chakshit629@gmail.com",
                    displayName = "Chakshit",
                    avatarColor = Color(0xFF1A73E8)
                )
            )
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Official Google 'G' Logo
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google",
                tint = Color.Unspecified,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Choose an account",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "to continue to Med Reminder",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account options list
            accounts.forEach { account ->
                Surface(
                    onClick = {
                        onDismissRequest()
                        onAccountSelected(account.email, account.displayName)
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(account.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.displayName.firstOrNull()?.uppercase() ?: "G",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = account.email,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // "Use another account"
            Surface(
                onClick = { showCustomEmailDialog = true },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Use another account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Official Google Terms & Privacy Disclaimer
            Text(
                text = "To continue, Google will share your name, email address, and profile picture with Med Reminder. Before using this app, review Med Reminder's Privacy Policy and Terms of Service.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
    }

    if (showCustomEmailDialog) {
        AlertDialog(
            onDismissRequest = { showCustomEmailDialog = false },
            title = { Text("Sign in with Google Account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your Google email address:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        placeholder = { Text("your.email@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customEmailInput.trim()
                        if (trimmed.isNotBlank()) {
                            showCustomEmailDialog = false
                            onDismissRequest()
                            val dispName = trimmed.substringBefore("@")
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            onAccountSelected(trimmed, dispName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
