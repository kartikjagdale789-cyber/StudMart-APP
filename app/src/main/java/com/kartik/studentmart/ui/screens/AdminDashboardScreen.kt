package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kartik.studentmart.data.model.ReportItem
import com.kartik.studentmart.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    productViewModel: ProductViewModel = viewModel()
) {
    val userProfile by productViewModel.userProfile.collectAsState()
    val allReports by productViewModel.allReports.collectAsState()

    var selectedTab by remember { mutableStateOf("All") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var productUnavailableMessage by remember { mutableStateOf<String?>(null) }

    val isAdmin = userProfile != null && userProfile!!.role.equals("ADMIN", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isAdmin && userProfile != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Access denied",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You do not have administrative privileges to view this dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Statistics Cards
                    val totalCount = allReports.size
                    val pendingCount = allReports.count { it.status.equals("PENDING", ignoreCase = true) }
                    val reviewedCount = allReports.count { it.status.equals("REVIEWED", ignoreCase = true) }
                    val resolvedCount = allReports.count { it.status.equals("RESOLVED", ignoreCase = true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Total", "$totalCount", Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f))
                        StatCard("Pending", "$pendingCount", Icons.Default.PendingActions, Modifier.weight(1f))
                        StatCard("Reviewed", "$reviewedCount", Icons.Default.RateReview, Modifier.weight(1f))
                        StatCard("Resolved", "$resolvedCount", Icons.Default.CheckCircle, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filters Row 1: Status
                    Text("Status Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "PENDING", "REVIEWED", "RESOLVED")) { tab ->
                            FilterChip(
                                selected = selectedTab.equals(tab, ignoreCase = true),
                                onClick = { selectedTab = tab },
                                label = { Text(tab) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters Row 2: Type
                    Text("Type Filter", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "PRODUCT", "USER")) { type ->
                            FilterChip(
                                selected = selectedType.equals(type, ignoreCase = true),
                                onClick = { selectedType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filtered Reports
                    val filteredReports = allReports.filter { r ->
                        val statusMatch = selectedTab.equals("All", ignoreCase = true) || r.status.equals(selectedTab, ignoreCase = true)
                        val typeMatch = selectedType.equals("All", ignoreCase = true) || r.type.equals(selectedType, ignoreCase = true)
                        statusMatch && typeMatch
                    }

                    Text(
                        text = "Reports (${filteredReports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredReports.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reports found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredReports, key = { it.reportId }) { report ->
                                ReportItemCard(
                                    report = report,
                                    onClick = { selectedReport = report }
                                )
                            }
                        }
                    }
                }
            }

            // Report Details Dialog
            if (selectedReport != null) {
                val report = selectedReport!!
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(report.createdAt))

                AlertDialog(
                    onDismissRequest = { selectedReport = null },
                    title = { Text("Report Details (${report.type})") },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Reason: ${report.reason}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (report.description.isNotBlank()) {
                                Text("Description: ${report.description}")
                            }
                            Text("Status: ${report.status}")
                            Text("Reporter: ${report.reporterName} (${report.reporterId})")
                            if (report.type == "PRODUCT") {
                                Text("Product Name: ${report.productName}")
                                Text("Product ID: ${report.productId}")
                            } else {
                                Text("Reported User ID: ${report.reportedUserId}")
                            }
                            Text("Date: $dateStr")

                            if (productUnavailableMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = productUnavailableMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (report.status.equals("PENDING", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            productViewModel.updateReportStatus(report.reportId, "REVIEWED")
                                            selectedReport = null
                                        }
                                    ) {
                                        Text("Mark as Reviewed")
                                    }
                                }
                                if (!report.status.equals("RESOLVED", ignoreCase = true)) {
                                    Button(
                                        onClick = {
                                            productViewModel.updateReportStatus(report.reportId, "RESOLVED")
                                            selectedReport = null
                                        }
                                    ) {
                                        Text("Mark as Resolved")
                                    }
                                }
                            }

                            if (report.type == "PRODUCT" && report.productId.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        productUnavailableMessage = null
                                        val pId = report.productId
                                        selectedReport = null
                                        onNavigateToDetails(pId)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Reported Product")
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            productUnavailableMessage = null
                            selectedReport = null
                        }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: String, imageVector: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReportItemCard(report: ReportItem, onClick: () -> Unit) {
    val statusColor = when (report.status.uppercase()) {
        "PENDING" -> MaterialTheme.colorScheme.error
        "REVIEWED" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = report.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.reason,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (report.type == "PRODUCT") "Product: ${report.productName}" else "Reported User: ${report.reportedUserId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Reporter: ${report.reporterName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor
            ) {
                Text(
                    text = report.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
