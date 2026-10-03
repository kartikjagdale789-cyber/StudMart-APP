package com.kartik.studentmart.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kartik.studentmart.data.model.Product
import com.kartik.studentmart.data.model.PublicProfile
import com.kartik.studentmart.data.model.ReportItem
import com.kartik.studentmart.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: String?,
    onNavigateToOffers: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToSell: (() -> Unit)? = null,
    productViewModel: ProductViewModel = viewModel()
) {
    val product = productViewModel.currentProduct
    val isLoading = productViewModel.isDetailsLoading
    val error = productViewModel.detailsErrorMessage
    val wishlistIds by productViewModel.wishlistProductIds.collectAsState()
    val activeUserProducts by productViewModel.userProducts.collectAsState()
    val blockedUserIds by productViewModel.blockedUserIds.collectAsState()
    val isActionLoading = productViewModel.isLoading

    var showLoginDialog by remember { mutableStateOf(false) }

    var showMakeOfferDialog by remember { mutableStateOf(false) }
    var offerPriceText by remember { mutableStateOf("") }
    var offerMessageText by remember { mutableStateOf("") }

    var showExchangeDialog by remember { mutableStateOf(false) }
    var selectedOfferedProduct by remember { mutableStateOf<Product?>(null) }
    var exchangeMessageText by remember { mutableStateOf("") }

    var showOfferSuccessDialog by remember { mutableStateOf(false) }

    // Report & Block dialog states
    var showReportProductDialog by remember { mutableStateOf(false) }
    var showReportUserDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showUnblockDialog by remember { mutableStateOf(false) }

    var reportReason by remember { mutableStateOf("Fraud / Scam") }
    var reportDescription by remember { mutableStateOf("") }
    var reportSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        if (productId != null) {
            productViewModel.loadProductDetails(productId)
        }
    }

    val isWishlisted = productId != null && wishlistIds.contains(productId)
    val currentUserId = productViewModel.currentUserId
    val isOwner = product != null && currentUserId != null && product.sellerId == currentUserId
    val isBlocked = product != null && blockedUserIds.contains(product.sellerId)

    // Fetch public seller profile with a stable LaunchedEffect and loading state tracking
    val sellerId = product?.sellerId ?: ""
    var sellerProfile by remember(sellerId) { mutableStateOf<PublicProfile?>(null) }
    var hasLoadedSellerProfile by remember(sellerId) { mutableStateOf(false) }

    LaunchedEffect(sellerId) {
        if (sellerId.isNotBlank()) {
            productViewModel.getPublicProfileFlow(sellerId).collect { profile ->
                sellerProfile = profile
                hasLoadedSellerProfile = true
            }
        } else {
            hasLoadedSellerProfile = true
        }
    }

    val canMakeExchangeOffer = product != null &&
            product.category.equals("Books", ignoreCase = true) &&
            product.exchangeAvailable &&
            product.status == "ACTIVE" &&
            !isOwner &&
            !isBlocked

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Product Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (product != null) {
                            IconButton(
                                onClick = {
                                    if (currentUserId == null) {
                                        showLoginDialog = true
                                    } else {
                                        reportReason = "Fraud / Scam"
                                        reportDescription = ""
                                        showReportProductDialog = true
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Report, contentDescription = "Report Product", tint = MaterialTheme.colorScheme.error)
                            }

                            IconButton(
                                onClick = {
                                    productViewModel.toggleWishlist(
                                        product.productId,
                                        onLoginRequired = { showLoginDialog = true }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
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
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (error != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { productId?.let { productViewModel.loadProductDetails(it) } }) {
                            Text("Retry")
                        }
                    }
                } else if (product == null) {
                    Text(text = "Product not found.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Product Images Slider / Pager
                        if (product.imageUrls.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { product.imageUrls.size })
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    AsyncImage(
                                        model = product.imageUrls[page],
                                        contentDescription = product.productName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Image Page Indicator Badge (e.g. 1 / 3)
                                if (product.imageUrls.size > 1) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = "${pagerState.currentPage + 1} / ${product.imageUrls.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Product Title and Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.productName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    productViewModel.toggleWishlist(
                                        product.productId,
                                        onLoginRequired = { showLoginDialog = true }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "₹${product.price}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category & Condition Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = {}, label = { Text("Category: ${product.category}") })
                            AssistChip(onClick = {}, label = { Text("Condition: ${product.condition}") })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Location: ${product.location}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (product.createdAt > 0) {
                            val postedDateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(product.createdAt))
                            Text(
                                text = "Posted on: $postedDateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Exchange Availability Tag: ONLY shown for Books when exchangeAvailable == true
                        if (product.category.equals("Books", ignoreCase = true) && product.exchangeAvailable) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Available for Exchange",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Seller Information & Contact Card
                        val sellerPhone = sellerProfile?.phoneNumber ?: ""
                        val sellerNameDisplay = when {
                            sellerProfile?.fullName?.isNotBlank() == true -> sellerProfile!!.fullName
                            product.sellerName.isNotBlank() -> product.sellerName
                            !hasLoadedSellerProfile -> "Loading..."
                            else -> "Seller information unavailable"
                        }
                        val sellerPhoneDisplay = when {
                            sellerPhone.isNotBlank() -> sellerPhone
                            !hasLoadedSellerProfile -> "Loading contact number..."
                            else -> "Contact number not available"
                        }
                        val sellerPhoto = sellerProfile?.profileImageUrl ?: ""
                        val context = LocalContext.current

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (sellerPhoto.isNotBlank()) {
                                            AsyncImage(
                                                model = sellerPhoto,
                                                contentDescription = "Seller Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = "Seller",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = sellerNameDisplay,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Contact Number Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable(enabled = sellerPhone.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sellerPhone"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("StudMartDialer", "Failed to launch dialer", e)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        tint = if (sellerPhone.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Contact Number",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = sellerPhoneDisplay,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (sellerPhone.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (sellerPhone.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                // Report User & Block User Actions (if not owner)
                                if (!isOwner) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                if (currentUserId == null) {
                                                    showLoginDialog = true
                                                } else {
                                                    reportReason = "Fraud / Scam"
                                                    reportDescription = ""
                                                    showReportUserDialog = true
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Report User", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                        }

                                        TextButton(
                                            onClick = {
                                                if (currentUserId == null) {
                                                    showLoginDialog = true
                                                } else if (isBlocked) {
                                                    showUnblockDialog = true
                                                } else {
                                                    showBlockDialog = true
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBlocked) "Unblock User" else "Block User",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.description.ifBlank { "No description provided." },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (productViewModel.errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = productViewModel.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (reportSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = reportSuccessMessage!!,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // CHAT WITH SELLER / MAKE OFFER / OWNER / SOLD STATUS BUTTONS
                        if (product.status == "SOLD") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "SOLD - This product is no longer available.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (isOwner) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "This is your product listing.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (isBlocked) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "You have blocked this user. Unblock them to interact.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentUserId == null) {
                                        showLoginDialog = true
                                    } else {
                                        productViewModel.getOrCreateChat(
                                            product = product,
                                            onSuccess = { chatId -> onNavigateToChat(chatId) }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = !isActionLoading
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat with Seller")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentUserId == null) {
                                            showLoginDialog = true
                                        } else {
                                            offerPriceText = ""
                                            offerMessageText = ""
                                            showMakeOfferDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Make Offer")
                                }

                                if (canMakeExchangeOffer) {
                                    OutlinedButton(
                                        onClick = {
                                            if (currentUserId == null) {
                                                showLoginDialog = true
                                            } else {
                                                selectedOfferedProduct = null
                                                exchangeMessageText = ""
                                                showExchangeDialog = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Exchange Offer")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Login Required Dialog
            if (showLoginDialog) {
                AlertDialog(
                    onDismissRequest = { showLoginDialog = false },
                    title = { Text("Login Required") },
                    text = { Text("Please log in to chat with the seller, make an offer, or save wishlist items.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLoginDialog = false
                                onNavigateToLogin?.invoke()
                            }
                        ) {
                            Text("Login")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLoginDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Report Product Dialog
            if (showReportProductDialog && product != null) {
                val reasons = listOf("Fraud / Scam", "Fake Product", "Wrong Information", "Inappropriate Content", "Duplicate Listing", "Other")
                AlertDialog(
                    onDismissRequest = { showReportProductDialog = false },
                    title = { Text("Report Product") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("Select reason:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            reasons.forEach { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = reportReason == r,
                                            onClick = { reportReason = r }
                                        )
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = reportReason == r,
                                        onClick = { reportReason = r }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(r, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            if (reportReason == "Other") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reportDescription,
                                    onValueChange = { reportDescription = it },
                                    label = { Text("Description *") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val report = ReportItem(
                                    reportedUserId = product.sellerId,
                                    productId = product.productId,
                                    productName = product.productName,
                                    reason = reportReason,
                                    description = reportDescription.trim(),
                                    type = "PRODUCT"
                                )
                                showReportProductDialog = false
                                productViewModel.submitReport(report) {
                                    reportSuccessMessage = "Report submitted successfully."
                                }
                            }
                        ) {
                            Text("Submit Report")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportProductDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Report User Dialog
            if (showReportUserDialog && product != null) {
                val reasons = listOf("Fraud / Scam", "Harassment", "Fake Profile", "Inappropriate Behaviour", "Other")
                AlertDialog(
                    onDismissRequest = { showReportUserDialog = false },
                    title = { Text("Report User") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("Select reason:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            reasons.forEach { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = reportReason == r,
                                            onClick = { reportReason = r }
                                        )
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = reportReason == r,
                                        onClick = { reportReason = r }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(r, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            if (reportReason == "Other") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reportDescription,
                                    onValueChange = { reportDescription = it },
                                    label = { Text("Description *") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val report = ReportItem(
                                    reportedUserId = product.sellerId,
                                    reason = reportReason,
                                    description = reportDescription.trim(),
                                    type = "USER"
                                )
                                showReportUserDialog = false
                                productViewModel.submitReport(report) {
                                    reportSuccessMessage = "Report submitted successfully."
                                }
                            }
                        ) {
                            Text("Submit Report")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportUserDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Block Confirmation Dialog
            if (showBlockDialog && product != null) {
                AlertDialog(
                    onDismissRequest = { showBlockDialog = false },
                    title = { Text("Block User?") },
                    text = { Text("Are you sure you want to block this user?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBlockDialog = false
                                productViewModel.blockUser(product.sellerId) {
                                    reportSuccessMessage = "User blocked successfully."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Block")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBlockDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Unblock Confirmation Dialog
            if (showUnblockDialog && product != null) {
                AlertDialog(
                    onDismissRequest = { showUnblockDialog = false },
                    title = { Text("Unblock User?") },
                    text = { Text("Are you sure you want to unblock this user?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUnblockDialog = false
                                productViewModel.unblockUser(product.sellerId) {
                                    reportSuccessMessage = "User unblocked successfully."
                                }
                            }
                        ) {
                            Text("Unblock")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnblockDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Make Sale Offer Dialog
            if (showMakeOfferDialog && product != null) {
                AlertDialog(
                    onDismissRequest = { showMakeOfferDialog = false },
                    title = { Text("Make Sale Offer") },
                    text = {
                        Column {
                            Text(text = product.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Listed Price: ₹${product.price}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = offerPriceText,
                                onValueChange = { offerPriceText = it },
                                label = { Text("Your Offer (₹) *") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = offerMessageText,
                                onValueChange = { offerMessageText = it },
                                label = { Text("Message (Optional)") },
                                placeholder = { Text("e.g. Can you sell it for ₹35,000?") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showMakeOfferDialog = false
                                productViewModel.createOffer(
                                    product = product,
                                    offerPriceStr = offerPriceText,
                                    message = offerMessageText,
                                    onSuccess = { showOfferSuccessDialog = true }
                                )
                            },
                            enabled = !isActionLoading
                        ) {
                            Text("Send Offer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMakeOfferDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Exchange Offer Dialog
            if (showExchangeDialog && product != null) {
                var dropdownExpanded by remember { mutableStateOf(false) }

                val eligibleProducts = activeUserProducts.filter {
                    it.category.equals("Books", ignoreCase = true) &&
                    it.status == "ACTIVE" &&
                    it.productId != product.productId
                }

                if (eligibleProducts.isEmpty()) {
                    AlertDialog(
                        onDismissRequest = { showExchangeDialog = false },
                        title = { Text("Make Exchange Offer") },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📚 Sell a Book First",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You need to list your book on StudentMart before you can make an exchange offer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        showExchangeDialog = false
                                        onNavigateToSell?.invoke()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("List My Book")
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showExchangeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                } else {
                    AlertDialog(
                        onDismissRequest = { showExchangeDialog = false },
                        title = { Text("Make Exchange Offer") },
                        text = {
                            Column {
                                Text(text = "You Want:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = product.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "Your Book to Offer:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))

                                ExposedDropdownMenuBox(
                                    expanded = dropdownExpanded,
                                    onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedOfferedProduct?.productName ?: "Select your book...",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false }
                                    ) {
                                        eligibleProducts.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("${p.productName} (₹${p.price})") },
                                                onClick = {
                                                    selectedOfferedProduct = p
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = exchangeMessageText,
                                    onValueChange = { exchangeMessageText = it },
                                    label = { Text("Message (Optional)") },
                                    placeholder = { Text("e.g. I would like to exchange these books.") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val offeredP = selectedOfferedProduct
                                    if (offeredP != null) {
                                        showExchangeDialog = false
                                        productViewModel.createExchangeOffer(
                                            requestedProduct = product,
                                            offeredProduct = offeredP,
                                            message = exchangeMessageText,
                                            onSuccess = { showOfferSuccessDialog = true }
                                        )
                                    }
                                },
                                enabled = selectedOfferedProduct != null && !isActionLoading
                            ) {
                                Text("Send Exchange Offer")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExchangeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Offer Success Dialog
            if (showOfferSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showOfferSuccessDialog = false },
                    title = { Text("Offer Sent!") },
                    text = { Text("Your offer has been submitted and linked to chat and offers.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOfferSuccessDialog = false
                                onNavigateToOffers()
                            }
                        ) {
                            Text("View My Offers")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOfferSuccessDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
