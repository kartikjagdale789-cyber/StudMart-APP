package com.kartik.studentmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kartik.studentmart.data.model.CategoryItem
import com.kartik.studentmart.ui.components.FilterBottomSheet
import com.kartik.studentmart.ui.components.ProductCard
import com.kartik.studentmart.viewmodel.FilterState
import com.kartik.studentmart.viewmodel.ProductViewModel
import com.kartik.studentmart.viewmodel.SortOrder

val sampleCategories = listOf(
    CategoryItem("1", "Books", Icons.AutoMirrored.Filled.MenuBook),
    CategoryItem("2", "Electronics", Icons.Default.Laptop),
    CategoryItem("3", "Accessories", Icons.Default.Watch),
    CategoryItem("4", "Bags", Icons.Default.ShoppingBag),
    CategoryItem("5", "Cycles", Icons.AutoMirrored.Filled.DirectionsBike),
    CategoryItem("6", "Clothes", Icons.Default.Checkroom),
    CategoryItem("7", "Calculators", Icons.Default.Calculate),
    CategoryItem("8", "Furniture", Icons.Default.Chair),
    CategoryItem("9", "Hostel Items", Icons.Default.Bed),
    CategoryItem("10", "Mobile Accessories", Icons.Default.PhoneAndroid),
    CategoryItem("11", "Other", Icons.Default.Category)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProductDetails: (String) -> Unit,
    onNavigateToBookExchange: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSell: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onNavigateToLogin: (() -> Unit)? = null,
    productViewModel: ProductViewModel = viewModel()
) {
    val filterState by productViewModel.filterState.collectAsState()
    val filteredProducts by productViewModel.filteredProducts.collectAsState()
    val wishlistIds by productViewModel.wishlistProductIds.collectAsState()
    val unreadCount by productViewModel.unreadNotificationCount.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header / Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SM",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "StudMart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Student Marketplace",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat")
                    }
                    Box {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                        }
                        if (unreadCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                            ) {
                                Text("$unreadCount")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar & Filter Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { productViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search products...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { productViewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (filterState.isFiltered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (filterState.isFiltered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (filterState.isFiltered) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                }
            }

            // Active Filter Chips Bar
            if (filterState.isFiltered) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (filterState.category.isNotBlank() && !filterState.category.equals("All", ignoreCase = true)) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateCategoryFilter("All") },
                                    label = { Text("Cat: ${filterState.category}") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (filterState.minPrice != null || filterState.maxPrice != null) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateFilterState(filterState.copy(minPrice = null, maxPrice = null)) },
                                    label = { Text("Price: ${filterState.minPrice?.toInt() ?: 0} - ${filterState.maxPrice?.toInt() ?: "Max"}") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (filterState.condition.isNotBlank() && !filterState.condition.equals("All", ignoreCase = true)) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateFilterState(filterState.copy(condition = "All")) },
                                    label = { Text("Cond: ${filterState.condition}") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (filterState.location.isNotBlank()) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateFilterState(filterState.copy(location = "")) },
                                    label = { Text("Loc: ${filterState.location}") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (filterState.exchangeAvailableOnly) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateFilterState(filterState.copy(exchangeAvailableOnly = false)) },
                                    label = { Text("Exchange Only") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (filterState.sortOrder != SortOrder.NEWEST) {
                            item {
                                InputChip(
                                    selected = true,
                                    onClick = { productViewModel.updateFilterState(filterState.copy(sortOrder = SortOrder.NEWEST)) },
                                    label = {
                                        Text(if (filterState.sortOrder == SortOrder.PRICE_LOW_TO_HIGH) "Price: Low→High" else "Price: High→Low")
                                    },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }

                    TextButton(onClick = { productViewModel.clearFilters() }) {
                        Text("Clear All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToCategories) {
                    Text("See All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Categories Horizontal Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleCategories) { category ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable {
                                productViewModel.updateCategoryFilter(category.name)
                                onCategoryClick(category.name)
                            }
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (filterState.category.equals(category.name, ignoreCase = true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.name,
                                    tint = if (filterState.category.equals(category.name, ignoreCase = true)) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Products Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filterState.isFiltered) "Marketplace Products (${filteredProducts.size})" else "Recent Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Empty State handling
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try changing your search or filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (filterState.isFiltered) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { productViewModel.clearFilters() }) {
                                Text("Clear Filters")
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onNavigateToProductDetails(product.productId) },
                            isWishlisted = wishlistIds.contains(product.productId),
                            onWishlistClick = {
                                productViewModel.toggleWishlist(
                                    product.productId,
                                    onLoginRequired = { onNavigateToLogin?.invoke() }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Book Exchange Quick Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToBookExchange),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Campus Book Exchange",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Exchange your course books with fellow students!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Filter Sheet Dialog
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilterState = filterState,
                onApply = { newFilters ->
                    productViewModel.updateFilterState(newFilters)
                },
                onClear = {
                    productViewModel.clearFilters()
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}
