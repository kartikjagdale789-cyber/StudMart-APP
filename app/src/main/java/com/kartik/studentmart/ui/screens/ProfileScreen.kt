package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kartik.studentmart.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToListings: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToReceivedOffers: () -> Unit,
    onNavigateToSellerRequests: () -> Unit,
    onNavigateToMyChats: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Profile & Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToListings,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("My Listings")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToMyChats,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("My Chats")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToPurchases,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("My Purchases")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToOffers,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("My Offers Sent")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToReceivedOffers,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Offers Received")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToSellerRequests,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Incoming Purchase Requests")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToAdmin,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Admin Dashboard")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    authViewModel.logout(onLogout)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Logout")
            }
        }
    }
}
