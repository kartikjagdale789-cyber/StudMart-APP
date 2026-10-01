package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kartik.studentmart.data.model.Offer
import com.kartik.studentmart.data.model.Product
import com.kartik.studentmart.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String?,
    onBack: () -> Unit,
    onNavigateToSell: (() -> Unit)? = null,
    productViewModel: ProductViewModel = viewModel()
) {
    val currentUserId = productViewModel.currentUserId
    val chat = productViewModel.currentChat
    val messages = productViewModel.currentChatMessages
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var showMakeOfferDialog by remember { mutableStateOf(false) }
    var offerPriceText by remember { mutableStateOf("") }
    var offerMessageText by remember { mutableStateOf("") }

    var showExchangeDialog by remember { mutableStateOf(false) }
    var selectedOfferedProduct by remember { mutableStateOf<Product?>(null) }
    var exchangeMessageText by remember { mutableStateOf("") }

    val activeProducts by productViewModel.activeProducts.collectAsState()
    val activeUserProducts by productViewModel.userProducts.collectAsState()
    val isActionLoading = productViewModel.isLoading

    LaunchedEffect(chatId) {
        if (!chatId.isNullOrBlank()) {
            productViewModel.openChat(chatId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val otherPersonName = if (chat != null && currentUserId != null) {
        if (chat.buyerId == currentUserId) chat.sellerName else chat.buyerName
    } else "User"

    val isBuyer = chat != null && currentUserId != null && chat.buyerId == currentUserId

    val chatProduct = if (chat != null) activeProducts.firstOrNull { it.productId == chat.productId } else null
    val canMakeExchangeOffer = isBuyer &&
            chat != null &&
            chat.status == "ACTIVE" &&
            chatProduct != null &&
            chatProduct.category.equals("Books", ignoreCase = true) &&
            chatProduct.exchangeAvailable &&
            chatProduct.status == "ACTIVE"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (chat != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (chat.productImage.isNotBlank()) {
                                        AsyncImage(
                                            model = chat.productImage,
                                            contentDescription = chat.productName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = chat.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₹${chat.productPrice} • $otherPersonName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text("Chat", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                if (chat != null && chatId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Quick Action Buttons
                            if (isBuyer && chat.status == "ACTIVE") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            offerPriceText = ""
                                            offerMessageText = ""
                                            showMakeOfferDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Make Offer", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (canMakeExchangeOffer) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedOfferedProduct = null
                                                exchangeMessageText = ""
                                                showExchangeDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Exchange Offer", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Message...") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val text = messageText.trim()
                                        if (text.isNotBlank()) {
                                            messageText = ""
                                            productViewModel.sendMessage(chatId, text)
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (chat == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Loading chat...")
                    }
                } else if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No messages yet. Say hello!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.senderId == currentUserId
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(msg.createdAt))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                if (msg.type == "offer" && msg.offerId.isNotBlank()) {
                                    ChatOfferCard(
                                        offerId = msg.offerId,
                                        currentUserId = currentUserId ?: "",
                                        productViewModel = productViewModel
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 16.dp
                                        ),
                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = (if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sale Offer Dialog
            if (showMakeOfferDialog && chat != null) {
                AlertDialog(
                    onDismissRequest = { showMakeOfferDialog = false },
                    title = { Text("Make Sale Offer") },
                    text = {
                        Column {
                            Text(text = chat.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Original Price: ₹${chat.productPrice}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                val dummyProduct = Product(
                                    productId = chat.productId,
                                    productName = chat.productName,
                                    sellerId = chat.sellerId,
                                    sellerName = chat.sellerName,
                                    price = chat.productPrice,
                                    imageUrls = if (chat.productImage.isNotBlank()) listOf(chat.productImage) else emptyList()
                                )
                                showMakeOfferDialog = false
                                productViewModel.createOffer(
                                    product = dummyProduct,
                                    offerPriceStr = offerPriceText,
                                    message = offerMessageText,
                                    onSuccess = { }
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
            if (showExchangeDialog && chat != null && chatProduct != null) {
                var dropdownExpanded by remember { mutableStateOf(false) }

                val eligibleProducts = activeUserProducts.filter {
                    it.category.equals("Books", ignoreCase = true) &&
                    it.status == "ACTIVE" &&
                    it.productId != chat.productId
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
                                Text(text = chat.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                                            requestedProduct = chatProduct,
                                            offeredProduct = offeredP,
                                            message = exchangeMessageText,
                                            onSuccess = { }
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
        }
    }
}

@Composable
fun ChatOfferCard(
    offerId: String,
    currentUserId: String,
    productViewModel: ProductViewModel
) {
    val offerState by productViewModel.getOfferByIdFlow(offerId).collectAsState(initial = null)
    val offer = offerState

    if (offer == null) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("Loading offer details...", style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        val isSeller = currentUserId == offer.sellerId
        val statusColor = when (offer.status) {
            "ACCEPTED" -> MaterialTheme.colorScheme.primary
            "PENDING" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }

        Card(
            modifier = Modifier
                .width(280.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (offer.type == "EXCHANGE") "🔄 Exchange Offer" else "💰 Sale Offer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor
                    ) {
                        Text(
                            text = offer.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (offer.type == "EXCHANGE") {
                    Text(
                        text = "Wants: ${offer.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Offers: ${offer.offeredProductName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = offer.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Listed: ₹${offer.listedPrice}  •  Offer: ₹${offer.offerPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (offer.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${offer.message}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                if (isSeller && offer.status == "PENDING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { productViewModel.rejectOffer(offer.offerId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (offer.type == "EXCHANGE") {
                                    productViewModel.acceptExchangeOffer(offer.offerId)
                                } else {
                                    productViewModel.acceptOffer(offer.offerId, offer.productId)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Text(
                        text = when (offer.status) {
                            "ACCEPTED" -> "Offer Accepted"
                            "REJECTED" -> "Offer Rejected"
                            "CANCELLED" -> "Offer Cancelled"
                            else -> "Offer Sent"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
