package com.kartik.studentmart.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kartik.studentmart.ui.screens.sampleCategories
import com.kartik.studentmart.viewmodel.FilterState
import com.kartik.studentmart.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilterState: FilterState,
    onApply: (FilterState) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(currentFilterState.category) }
    var minPriceText by remember { mutableStateOf(currentFilterState.minPrice?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var maxPriceText by remember { mutableStateOf(currentFilterState.maxPrice?.let { if (it % 1 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var condition by remember { mutableStateOf(currentFilterState.condition) }
    var location by remember { mutableStateOf(currentFilterState.location) }
    var exchangeOnly by remember { mutableStateOf(currentFilterState.exchangeAvailableOnly) }
    var sortOrder by remember { mutableStateOf(currentFilterState.sortOrder) }

    val categories = listOf("All") + sampleCategories.map { it.name }
    val conditions = listOf("All", "New", "Like New", "Good", "Fair", "Poor")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Filters & Sorting", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort By Section
            Text(text = "Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortOrder == SortOrder.NEWEST,
                    onClick = { sortOrder = SortOrder.NEWEST },
                    label = { Text("Newest") }
                )
                FilterChip(
                    selected = sortOrder == SortOrder.PRICE_LOW_TO_HIGH,
                    onClick = { sortOrder = SortOrder.PRICE_LOW_TO_HIGH },
                    label = { Text("Price: Low to High") }
                )
                FilterChip(
                    selected = sortOrder == SortOrder.PRICE_HIGH_TO_LOW,
                    onClick = { sortOrder = SortOrder.PRICE_HIGH_TO_LOW },
                    label = { Text("Price: High to Low") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Section
            Text(text = "Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price Range Section
            Text(text = "Price Range (₹)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minPriceText,
                    onValueChange = { minPriceText = it },
                    label = { Text("Min Price") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxPriceText,
                    onValueChange = { maxPriceText = it },
                    label = { Text("Max Price") },
                    placeholder = { Text("Any") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Condition Section
            Text(text = "Condition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conditions.forEach { cond ->
                    FilterChip(
                        selected = condition.equals(cond, ignoreCase = true),
                        onClick = { condition = cond },
                        label = { Text(cond) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Section
            Text(text = "Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location / Campus Block") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exchange Available Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Exchange Available Only", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Applies only to Books with exchange available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = exchangeOnly,
                    onCheckedChange = { exchangeOnly = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onClear()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Filters")
                }

                Button(
                    onClick = {
                        val minP = minPriceText.toDoubleOrNull()
                        val maxP = maxPriceText.toDoubleOrNull()
                        val newFilterState = currentFilterState.copy(
                            category = category,
                            minPrice = minP,
                            maxPrice = maxP,
                            condition = condition,
                            location = location,
                            exchangeAvailableOnly = exchangeOnly,
                            sortOrder = sortOrder
                        )
                        onApply(newFilterState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }
}
