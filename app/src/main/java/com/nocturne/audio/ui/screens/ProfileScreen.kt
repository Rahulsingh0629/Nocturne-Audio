package com.nocturne.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nocturne.audio.ui.components.AmbientBackground
import com.nocturne.audio.ui.profile.ProfileViewModel
import com.nocturne.audio.ui.theme.AccentPurple
import com.nocturne.audio.ui.theme.BackgroundDark
import com.nocturne.audio.ui.theme.BorderSubtle
import com.nocturne.audio.ui.theme.SurfaceDark
import com.nocturne.audio.ui.theme.TextPrimary
import com.nocturne.audio.ui.theme.TextSecondary
import com.nocturne.audio.ui.theme.TextTertiary

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAccountDeleted) {
        if (uiState.isAccountDeleted) {
            onSignOut()
        }
    }

    var currentPassInput by remember { mutableStateOf("") }
    var newPassInput by remember { mutableStateOf("") }
    var deleteConfirmPassInput by remember { mutableStateOf("") }

    val availableLanguages = listOf("English", "Hindi", "Punjabi", "Spanish", "Tamil", "Korean", "Japanese")
    val availableDecades = listOf("2020s", "2010s", "2000s", "1990s", "1980s", "1970s", "1960s")

    AmbientBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar Banner
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .border(2.dp, AccentPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.profile.name.ifBlank { "User Profile" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Text(
                    text = uiState.profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info / Message Banners
                uiState.message?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentPurple.copy(alpha = 0.2f))
                            .border(1.dp, AccentPurple, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = AccentPurple)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                uiState.errorMessage?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp)
                    ) {
                        Text(text = err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Edit Display Name
                OutlinedTextField(
                    value = uiState.editedName,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Display Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    colors = profileTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email (Read-only)
                OutlinedTextField(
                    value = uiState.profile.email,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    colors = profileTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Preferred Languages Selection
                Text(
                    text = "Preferred Languages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                SelectableGrid(
                    items = availableLanguages,
                    selectedItems = uiState.selectedLanguages,
                    onToggle = { viewModel.toggleLanguage(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preferred Decades Selection
                Text(
                    text = "Preferred Decades",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                SelectableGrid(
                    items = availableDecades,
                    selectedItems = uiState.selectedDecades,
                    onToggle = { viewModel.toggleDecade(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Changes Button
                Button(
                    onClick = { viewModel.saveProfileChanges() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = BackgroundDark)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundDark)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                Spacer(modifier = Modifier.height(24.dp))

                // Account Security Actions
                Text(
                    text = "Account Security & Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Change Password Button
                OutlinedButton(
                    onClick = {
                        currentPassInput = ""
                        newPassInput = ""
                        viewModel.toggleChangePasswordDialog(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Privacy Policy Link
                OutlinedButton(
                    onClick = { viewModel.togglePrivacyPolicyDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy Policy & Terms", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text("Sign Out", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Delete Account Button (Red)
                OutlinedButton(
                    onClick = {
                        deleteConfirmPassInput = ""
                        viewModel.toggleDeleteAccountDialog(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Change Password Dialog (Re-authentication required)
        if (uiState.showChangePasswordDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleChangePasswordDialog(false) },
                title = { Text("Change Password", color = TextPrimary) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = currentPassInput,
                            onValueChange = { currentPassInput = it },
                            label = { Text("Current Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPassInput,
                            onValueChange = { newPassInput = it },
                            label = { Text("New Password (min 8 chars)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.changePassword(currentPassInput, newPassInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Update", color = BackgroundDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleChangePasswordDialog(false) }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // Delete Account Dialog (Re-authentication required)
        if (uiState.showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleDeleteAccountDialog(false) },
                title = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                text = {
                    Column {
                        Text(
                            text = "Warning: This action will permanently delete your profile, playlists, favorites, and cloud uploads. Please enter your password to confirm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = deleteConfirmPassInput,
                            onValueChange = { deleteConfirmPassInput = it },
                            label = { Text("Password to Confirm") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteAccount(deleteConfirmPassInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permanently Delete", color = TextPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.toggleDeleteAccountDialog(false) }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // Privacy Policy & Terms Dialog
        if (uiState.showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.togglePrivacyPolicyDialog(false) },
                title = { Text("Privacy Policy & Terms", color = TextPrimary) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Nocturne Audio Privacy & Content Policy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentPurple
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Data Collection: We collect only your name, email, music preferences, favorites, playlists, and uploaded audio metadata.\n\n" +
                                   "2. Cloud Uploads: Audio files uploaded by you are stored privately in Firebase Storage. They are strictly private to your account and never shared publicly.\n\n" +
                                   "3. Copyright & Ownership: You must own or hold rights to any audio file you upload.\n\n" +
                                   "4. JioSaavn Catalog: Main streaming content is sourced via JioSaavn API.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.togglePrivacyPolicyDialog(false) }) {
                        Text("I Understand", color = AccentPurple)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPurple,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = AccentPurple,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
private fun SelectableGrid(
    items: List<String>,
    selectedItems: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedItems.contains(item)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentPurple.copy(alpha = 0.2f) else SurfaceDark)
                            .border(1.dp, if (isSelected) AccentPurple else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onToggle(item) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = if (isSelected) AccentPurple else TextSecondary
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
