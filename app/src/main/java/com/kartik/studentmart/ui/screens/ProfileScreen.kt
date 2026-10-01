package com.kartik.studentmart.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kartik.studentmart.viewmodel.AuthViewModel
import com.kartik.studentmart.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToListings: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToReceivedOffers: () -> Unit,
    onNavigateToSellerRequests: () -> Unit,
    onNavigateToMyChats: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    productViewModel: ProductViewModel = viewModel()
) {
    val currentUserId = productViewModel.currentUserId
    val userProfile by productViewModel.userProfile.collectAsState()
    val isActionLoading = productViewModel.isLoading
    val coroutineScope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editProfileImageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isUploadingImage = true
            coroutineScope.launch {
                val uploadResult = productViewModel.uploadSingleImage(uri)
                isUploadingImage = false
                if (uploadResult.isSuccess) {
                    editProfileImageUrl = uploadResult.getOrNull() ?: ""
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile & Account", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            if (currentUserId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Please log in to view your profile.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLogout) { Text("Login") }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = userProfile?.profileImageUrl ?: ""
                                if (avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = userProfile?.fullName?.ifBlank { null } ?: "Student",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = userProfile?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = {
                                    productViewModel.clearMessages()
                                    editName = userProfile?.fullName ?: ""
                                    editProfileImageUrl = userProfile?.profileImageUrl ?: ""
                                    selectedImageUri = null
                                    showEditProfileDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Menu Options
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMenuRow(
                            icon = Icons.Default.Storefront,
                            title = "My Listings",
                            onClick = onNavigateToListings
                        )
                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = "My Chats",
                            onClick = onNavigateToMyChats
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.Favorite,
                            title = "Wishlist",
                            onClick = onNavigateToWishlist
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.LocalOffer,
                            title = "My Offers Sent",
                            onClick = onNavigateToOffers
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.Sell,
                            title = "Offers Received",
                            onClick = onNavigateToReceivedOffers
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.ShoppingCart,
                            title = "Incoming Purchase Requests",
                            onClick = onNavigateToSellerRequests
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = onNavigateToNotifications
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.Lock,
                            title = "Change Password",
                            onClick = {
                                productViewModel.clearMessages()
                                newPassword = ""
                                confirmPassword = ""
                                showChangePasswordDialog = true
                            }
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "Admin Dashboard",
                            onClick = onNavigateToAdmin
                        )
                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "Logout",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = {
                                authViewModel.logout(onLogout)
                            }
                        )
                        ProfileMenuRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Delete Account",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = {
                                productViewModel.clearMessages()
                                showDeleteAccountDialog = true
                            }
                        )
                    }
                }
            }

            // Edit Profile Dialog
            if (showEditProfileDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isActionLoading && !isUploadingImage) showEditProfileDialog = false
                    },
                    title = { Text("Edit Profile") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !isUploadingImage && !isActionLoading) {
                                        imagePickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                } else if (editProfileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = editProfileImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                enabled = !isUploadingImage && !isActionLoading
                            ) {
                                Text(if (isUploadingImage) "Uploading..." else "Change Photo")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name *") },
                                singleLine = true,
                                enabled = !isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (productViewModel.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = productViewModel.errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                productViewModel.updateUserProfile(editName, editProfileImageUrl) { success ->
                                    if (success) {
                                        showEditProfileDialog = false
                                    }
                                }
                            },
                            enabled = !isActionLoading && !isUploadingImage
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Save Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            enabled = !isActionLoading && !isUploadingImage
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Change Password Dialog
            if (showChangePasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isActionLoading) showChangePasswordDialog = false
                    },
                    title = { Text("Change Password") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("New Password *") },
                                singleLine = true,
                                enabled = !isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm New Password *") },
                                singleLine = true,
                                enabled = !isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (productViewModel.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = productViewModel.errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                productViewModel.updatePassword(newPassword, confirmPassword) {
                                    showChangePasswordDialog = false
                                }
                            },
                            enabled = !isActionLoading
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Update Password")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showChangePasswordDialog = false },
                            enabled = !isActionLoading
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Delete Account Confirmation Dialog
            if (showDeleteAccountDialog) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isActionLoading) showDeleteAccountDialog = false
                    },
                    title = { Text("Delete Account?") },
                    text = {
                        Column {
                            Text("This will permanently delete your account. This action cannot be undone.")
                            if (productViewModel.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = productViewModel.errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                productViewModel.deleteAccount {
                                    showDeleteAccountDialog = false
                                    onLogout()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = !isActionLoading
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onError)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Delete Account")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteAccountDialog = false },
                            enabled = !isActionLoading
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tint,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
