package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kartik.studentmart.data.model.NotificationItem
import com.kartik.studentmart.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToLogin: () -> Unit,
    productViewModel: ProductViewModel = viewModel()
) {
    val currentUserId = productViewModel.currentUserId
    val notifications by productViewModel.notifications.collectAsState()
    val unreadCount by productViewModel.unreadNotificationCount.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notifications", fontWeight = FontWeight.Bold)
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (currentUserId != null && notifications.isNotEmpty()) {
                            IconButton(onClick = { productViewModel.markAllNotificationsAsRead() }) {
                                Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (currentUserId == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Please log in to view notifications.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToLogin) {
                            Text("Login")
                        }
                    }
                } else if (notifications.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🔔 No notifications yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your offers, messages and activity updates will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications, key = { it.notificationId }) { item ->
                            NotificationCard(
                                notification = item,
                                onClick = {
                                    if (!item.isRead) {
                                        productViewModel.markNotificationAsRead(item.notificationId)
                                    }
                                    when (item.type) {
                                        "NEW_MESSAGE" -> {
                                            if (item.chatId.isNotBlank()) {
                                                onNavigateToChat(item.chatId)
                                            } else if (item.productId.isNotBlank()) {
                                                onNavigateToProductDetails(item.productId)
                                            }
                                        }
                                        "NEW_OFFER", "OFFER_ACCEPTED", "OFFER_REJECTED",
                                        "NEW_EXCHANGE_OFFER", "EXCHANGE_ACCEPTED", "EXCHANGE_REJECTED" -> {
                                            if (item.chatId.isNotBlank()) {
                                                onNavigateToChat(item.chatId)
                                            } else if (item.productId.isNotBlank()) {
                                                onNavigateToProductDetails(item.productId)
                                            } else {
                                                onNavigateToOffers()
                                            }
                                        }
                                        "PRODUCT_SOLD" -> {
                                            if (item.productId.isNotBlank()) {
                                                onNavigateToProductDetails(item.productId)
                                            }
                                        }
                                        else -> {
                                            if (item.productId.isNotBlank()) {
                                                onNavigateToProductDetails(item.productId)
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    productViewModel.deleteNotification(item.notificationId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (notification.type) {
        "NEW_OFFER", "OFFER_ACCEPTED", "OFFER_REJECTED" -> Icons.Default.LocalOffer
        "NEW_EXCHANGE_OFFER", "EXCHANGE_ACCEPTED", "EXCHANGE_REJECTED" -> Icons.Default.SwapHoriz
        "NEW_MESSAGE" -> Icons.AutoMirrored.Filled.Chat
        "PRODUCT_SOLD" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Notifications
    }

    val timeAgo = formatTimeAgo(notification.createdAt)
    val cardContainerColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatTimeAgo(timeMillis: Long): String {
    val diff = System.currentTimeMillis() - timeMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
        hours < 24 -> "$hours hr${if (hours > 1) "s" else ""} ago"
        days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
        else -> SimpleDateFormat("dd MMM", Locale.US).format(Date(timeMillis))
    }
}
