package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kartik.studentmart.data.model.Product
import com.kartik.studentmart.viewmodel.ProductViewModel

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
    val isActionLoading = productViewModel.isLoading

    var showLoginDialog by remember { mutableStateOf(false) }

    var showMakeOfferDialog by remember { mutableStateOf(false) }
    var offerPriceText by remember { mutableStateOf("") }
    var offerMessageText by remember { mutableStateOf("") }

    var showExchangeDialog by remember { mutableStateOf(false) }
    var selectedOfferedProduct by remember { mutableStateOf<Product?>(null) }
    var exchangeMessageText by remember { mutableStateOf("") }

    var showOfferSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (productId != null) {
            productViewModel.loadProductDetails(productId)
        }
    }

    val isWishlisted = productId != null && wishlistIds.contains(productId)
    val currentUserId = productViewModel.currentUserId
    val isOwner = product != null && currentUserId != null && product.sellerId == currentUserId

    val canMakeExchangeOffer = product != null &&
            product.category.equals("Books", ignoreCase = true) &&
            product.exchangeAvailable &&
            product.status == "ACTIVE" &&
            !isOwner

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
                        // Images Horizontal Row
                        if (product.imageUrls.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                items(product.imageUrls) { url ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(260.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = product.productName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
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

                        Text(
                            text = product.productName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${product.price}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                        Text(
                            text = "Seller: ${product.sellerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Exchange Availability Rule: ONLY shown for Books when exchangeAvailable == true
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // CHAT WITH SELLER / MAKE OFFER / OWNER / SOLD STATUS BUTTONS
                        if (product.status == "SOLD") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "SOLD - This product is no longer available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else if (isOwner) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "This is your product listing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
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
