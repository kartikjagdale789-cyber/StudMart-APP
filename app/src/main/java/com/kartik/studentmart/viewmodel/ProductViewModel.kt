package com.kartik.studentmart.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kartik.studentmart.data.model.Chat
import com.kartik.studentmart.data.model.ChatMessage
import com.kartik.studentmart.data.model.NotificationItem
import com.kartik.studentmart.data.model.Offer
import com.kartik.studentmart.data.model.Product
import com.kartik.studentmart.data.model.PublicProfile
import com.kartik.studentmart.data.model.PurchaseRequest
import com.kartik.studentmart.data.model.ReportItem
import com.kartik.studentmart.data.model.User
import com.kartik.studentmart.data.repository.StudMartRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW
}

data class FilterState(
    val searchQuery: String = "",
    val category: String = "All",
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val condition: String = "All",
    val location: String = "",
    val exchangeAvailableOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST
) {
    val isFiltered: Boolean
        get() = searchQuery.isNotBlank() ||
                (category.isNotBlank() && !category.equals("All", ignoreCase = true)) ||
                minPrice != null ||
                maxPrice != null ||
                (condition.isNotBlank() && !condition.equals("All", ignoreCase = true)) ||
                location.isNotBlank() ||
                exchangeAvailableOnly ||
                sortOrder != SortOrder.NEWEST
}

class ProductViewModel : ViewModel() {
    private val repository = StudMartRepository()

    // Flow that emits current user's UID whenever auth state changes
    private val currentUserIdFlow = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fAuth ->
            trySend(fAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId get() = repository.currentUserId

    // Public active products available for all users
    val activeProducts = repository.getActiveProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val blockedUserIds = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptySet()) else repository.getBlockedUserIdsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Filtering State
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredProducts: StateFlow<List<Product>> = combine(
        activeProducts,
        _filterState,
        blockedUserIds
    ) { products, filters, blockedIds ->
        products.filter { p ->
            // 1. ACTIVE status check
            if (p.status != "ACTIVE") return@filter false

            // 1b. Blocked seller check
            if (blockedIds.contains(p.sellerId)) return@filter false

            // 2. Search query (productName or description)
            if (filters.searchQuery.isNotBlank()) {
                val query = filters.searchQuery.trim().lowercase()
                val nameMatch = p.productName.lowercase().contains(query)
                val descMatch = p.description.lowercase().contains(query)
                if (!nameMatch && !descMatch) return@filter false
            }

            // 3. Category filter
            if (filters.category.isNotBlank() && !filters.category.equals("All", ignoreCase = true)) {
                if (!p.category.equals(filters.category, ignoreCase = true)) return@filter false
            }

            // 4. Price range filter
            if (filters.minPrice != null && p.price < filters.minPrice) return@filter false
            if (filters.maxPrice != null && p.price > filters.maxPrice) return@filter false

            // 5. Condition filter
            if (filters.condition.isNotBlank() && !filters.condition.equals("All", ignoreCase = true)) {
                if (!p.condition.equals(filters.condition, ignoreCase = true)) return@filter false
            }

            // 6. Location filter
            if (filters.location.isNotBlank()) {
                val locQuery = filters.location.trim().lowercase()
                if (!p.location.lowercase().contains(locQuery)) return@filter false
            }

            // 7. Exchange filter (ONLY relevant for Books)
            if (filters.exchangeAvailableOnly) {
                if (!p.category.equals("Books", ignoreCase = true) || !p.exchangeAvailable) return@filter false
            }

            true
        }.let { list ->
            // 8. Sorting
            when (filters.sortOrder) {
                SortOrder.NEWEST -> list.sortedByDescending { it.createdAt }
                SortOrder.PRICE_LOW_TO_HIGH -> list.sortedBy { it.price }
                SortOrder.PRICE_HIGH_TO_LOW -> list.sortedByDescending { it.price }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateCategoryFilter(category: String) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun updateFilterState(newFilters: FilterState) {
        _filterState.value = newFilters
    }

    fun clearFilters() {
        _filterState.value = FilterState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allReports = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getAllReportsFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateReportStatus(reportId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val result = repository.updateReportStatus(reportId, newStatus)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to update report status."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to update report status."
            }
        }
    }

    fun submitReport(report: ReportItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.submitReport(report)
                result.fold(
                    onSuccess = {
                        successMessage = "Report submitted successfully."
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to submit report."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to submit report."
            }
        }
    }

    fun blockUser(blockedUserId: String, onSuccess: () -> Unit) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val result = repository.blockUser(uid, blockedUserId)
                result.fold(
                    onSuccess = {
                        successMessage = "User blocked successfully."
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to block user."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to block user."
            }
        }
    }

    fun unblockUser(blockedUserId: String, onSuccess: () -> Unit) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                val result = repository.unblockUser(uid, blockedUserId)
                result.fold(
                    onSuccess = {
                        successMessage = "User unblocked successfully."
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to unblock user."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to unblock user."
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProducts = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getUserProductsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(null) else repository.getUserProfileFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun getUserProfileFlow(userId: String): Flow<User?> = repository.getUserProfileFlow(userId)

    fun getPublicProfileFlow(sellerId: String): Flow<PublicProfile?> = repository.getPublicProfileFlow(sellerId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val wishlistProductIds = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptySet()) else repository.getWishlistProductIdsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val wishlistProducts = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getWishlistProductsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val buyerRequests = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getBuyerPurchaseRequestsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sellerRequests = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getSellerPurchaseRequestsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val buyerOffers = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getBuyerOffersFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sellerOffers = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getSellerOffersFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userChats = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getUserChatsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else repository.getNotificationsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadNotificationCount = currentUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(0) else repository.getUnreadNotificationCountFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var currentProduct by mutableStateOf<Product?>(null)
        private set

    var isDetailsLoading by mutableStateOf(false)
        private set

    var detailsErrorMessage by mutableStateOf<String?>(null)
        private set

    // Active Chat State
    var activeChatId by mutableStateOf<String?>(null)
        private set

    var currentChat by mutableStateOf<Chat?>(null)
        private set

    var currentChatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set

    fun getOfferByIdFlow(offerId: String): Flow<Offer?> = repository.getOfferByIdFlow(offerId)

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(uid)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    fun updateUserProfile(fullName: String, phoneNumber: String, profileImageUrl: String, onResult: (Boolean) -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            errorMessage = "User not logged in."
            onResult(false)
            return
        }
        if (fullName.isBlank()) {
            errorMessage = "Name cannot be empty."
            onResult(false)
            return
        }
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank()) {
            errorMessage = "Mobile number cannot be empty."
            onResult(false)
            return
        }
        if (cleanPhone.length != 10 || !cleanPhone.all { it.isDigit() }) {
            errorMessage = "Please enter a valid 10-digit mobile number."
            onResult(false)
            return
        }

        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val result = repository.updateUserProfile(uid, fullName, cleanPhone, profileImageUrl)
                isLoading = false
                result.fold(
                    onSuccess = {
                        successMessage = "Profile updated successfully!"
                        onResult(true)
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to update profile."
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Failed to update profile."
                onResult(false)
            }
        }
    }

    fun updatePassword(newPass: String, confirmPass: String, onSuccess: () -> Unit) {
        if (newPass.isBlank() || newPass.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (newPass != confirmPass) {
            errorMessage = "Passwords do not match."
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val result = repository.updatePassword(newPass)
                result.fold(
                    onSuccess = {
                        successMessage = "Password updated successfully!"
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to update password. You may need to re-login."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to update password."
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val result = repository.deleteUserAccount()
                result.fold(
                    onSuccess = { onSuccess() },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to delete account. You may need to re-login."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to delete account."
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun uploadSingleImage(uri: Uri): Result<String> {
        val result = repository.uploadImages(listOf(uri))
        return if (result.isSuccess) {
            Result.success(result.getOrNull()?.firstOrNull() ?: "")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Upload failed"))
        }
    }

    fun getOrCreateChat(product: Product, onSuccess: (chatId: String) -> Unit) {
        if (currentUserId == null) {
            errorMessage = "Please log in to start a chat."
            return
        }
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val result = repository.getOrCreateChat(product)
                result.fold(
                    onSuccess = { chatId ->
                        activeChatId = chatId
                        onSuccess(chatId)
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to start chat."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to start chat."
            } finally {
                isLoading = false
            }
        }
    }

    fun openChat(chatId: String) {
        activeChatId = chatId
        viewModelScope.launch {
            repository.getChatByIdFlow(chatId).collect { chat ->
                currentChat = chat
            }
        }
        viewModelScope.launch {
            repository.getChatMessagesFlow(chatId).collect { messages ->
                currentChatMessages = messages
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.trim().isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, text)
            } catch (e: Exception) {
                Log.e("StudMartChat", "Failed to send message", e)
            }
        }
    }

    fun loadProductDetails(productId: String) {
        isDetailsLoading = true
        detailsErrorMessage = null
        currentProduct = null
        viewModelScope.launch {
            try {
                val result = repository.getProductById(productId)
                isDetailsLoading = false
                result.fold(
                    onSuccess = { product ->
                        currentProduct = product
                        if (product == null) {
                            detailsErrorMessage = "Product not found."
                        }
                    },
                    onFailure = { err ->
                        detailsErrorMessage = err.localizedMessage ?: "Failed to load product details."
                    }
                )
            } catch (e: Exception) {
                isDetailsLoading = false
                detailsErrorMessage = e.localizedMessage ?: "Failed to load product details."
            }
        }
    }

    fun toggleWishlist(productId: String, onLoginRequired: () -> Unit) {
        val uid = currentUserId
        if (uid == null) {
            onLoginRequired()
            return
        }
        viewModelScope.launch {
            try {
                if (wishlistProductIds.value.contains(productId)) {
                    repository.removeFromWishlist(uid, productId)
                } else {
                    repository.addToWishlist(uid, productId)
                }
            } catch (e: Exception) {
                Log.e("StudMartWishlist", "Failed to toggle wishlist", e)
            }
        }
    }

    fun removeFromWishlist(productId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.removeFromWishlist(uid, productId)
            } catch (e: Exception) {
                Log.e("StudMartWishlist", "Failed to remove from wishlist", e)
            }
        }
    }

    fun createOffer(
        product: Product,
        offerPriceStr: String,
        message: String,
        onSuccess: () -> Unit
    ) {
        if (currentUserId == null) {
            errorMessage = "Please log in to send an offer."
            return
        }
        val offerPrice = offerPriceStr.toDoubleOrNull()
        if (offerPrice == null || offerPrice <= 0.0) {
            errorMessage = "Offer price must be greater than 0."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                val result = repository.createOffer(product, offerPrice, message)
                result.fold(
                    onSuccess = {
                        successMessage = "Offer submitted successfully!"
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to submit offer."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to submit offer."
            } finally {
                isLoading = false
            }
        }
    }

    fun createExchangeOffer(
        requestedProduct: Product,
        offeredProduct: Product,
        message: String,
        onSuccess: () -> Unit
    ) {
        if (currentUserId == null) {
            errorMessage = "Please log in to send an exchange offer."
            return
        }
        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                val result = repository.createExchangeOffer(requestedProduct, offeredProduct, message)
                result.fold(
                    onSuccess = {
                        successMessage = "Exchange offer submitted successfully!"
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to submit exchange offer."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to submit exchange offer."
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelOffer(offerId: String) {
        viewModelScope.launch {
            try {
                val result = repository.cancelOffer(offerId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to cancel offer."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to cancel offer."
            }
        }
    }

    fun acceptOffer(offerId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = repository.acceptOffer(offerId, productId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to accept offer."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to accept offer."
            }
        }
    }

    fun acceptExchangeOffer(offerId: String) {
        viewModelScope.launch {
            try {
                val result = repository.acceptExchangeOffer(offerId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to accept exchange offer."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to accept exchange offer."
            }
        }
    }

    fun rejectOffer(offerId: String) {
        viewModelScope.launch {
            try {
                val result = repository.rejectOffer(offerId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to reject offer."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to reject offer."
            }
        }
    }

    fun createPurchaseRequest(
        product: Product,
        onSuccess: () -> Unit
    ) {
        if (currentUserId == null) {
            errorMessage = "Please log in to submit a purchase request."
            return
        }
        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                val result = repository.createPurchaseRequest(product)
                result.fold(
                    onSuccess = {
                        successMessage = "Purchase request submitted successfully!"
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.message ?: "Failed to submit purchase request."
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to submit purchase request."
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelPurchaseRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = repository.cancelPurchaseRequest(requestId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to cancel request."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to cancel request."
            }
        }
    }

    fun acceptPurchaseRequest(requestId: String, productId: String) {
        viewModelScope.launch {
            try {
                val result = repository.acceptPurchaseRequest(requestId, productId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to accept request."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to accept request."
            }
        }
    }

    fun rejectPurchaseRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = repository.rejectPurchaseRequest(requestId)
                result.onFailure { err ->
                    errorMessage = err.message ?: "Failed to reject request."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to reject request."
            }
        }
    }

    fun publishProduct(
        productName: String,
        description: String,
        priceStr: String,
        category: String,
        condition: String,
        location: String,
        imageUris: List<Uri>,
        exchangeAvailable: Boolean,
        onSuccess: () -> Unit
    ) {
        if (productName.isBlank()) {
            errorMessage = "Product name cannot be empty."
            return
        }
        if (description.isBlank()) {
            errorMessage = "Description cannot be empty."
            return
        }
        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            errorMessage = "Price must be greater than 0."
            return
        }
        if (category.isBlank()) {
            errorMessage = "Category must be selected."
            return
        }
        if (condition.isBlank()) {
            errorMessage = "Condition must be selected."
            return
        }
        if (location.isBlank()) {
            errorMessage = "Location cannot be empty."
            return
        }
        if (imageUris.isEmpty()) {
            errorMessage = "Please select at least one product image."
            return
        }

        isLoading = true
        errorMessage = null
        successMessage = null

        viewModelScope.launch {
            try {
                val uploadResult = repository.uploadImages(imageUris)
                if (uploadResult.isFailure) {
                    val err = uploadResult.exceptionOrNull()
                    val errorMsg = err?.message ?: "Failed to upload images"
                    Log.e("StudMartPublish", "Image upload failed: $errorMsg", err)
                    errorMessage = errorMsg
                    return@launch
                }

                val urls = uploadResult.getOrNull() ?: emptyList()
                if (urls.isEmpty()) {
                    errorMessage = "Failed to obtain image URLs from upload."
                    return@launch
                }

                val validExchange = if (category.equals("Books", ignoreCase = true)) exchangeAvailable else false
                val product = Product(
                    productName = productName.trim(),
                    description = description.trim(),
                    price = price,
                    category = category,
                    condition = condition,
                    location = location.trim(),
                    imageUrls = urls,
                    exchangeAvailable = validExchange
                )

                val createResult = repository.createProduct(product)
                if (createResult.isSuccess) {
                    successMessage = "Product listed successfully"
                    onSuccess()
                } else {
                    val err = createResult.exceptionOrNull()
                    val errorMsg = err?.message ?: "Failed to create product in database"
                    Log.e("StudMartPublish", "Firestore creation failed: $errorMsg", err)
                    errorMessage = errorMsg
                }
            } catch (e: Exception) {
                val errorType = e.javaClass.simpleName
                val errorMsg = e.message ?: "Unknown error"
                val errorCause = e.cause?.toString() ?: "None"
                Log.e("StudMartPublish", "Publish exception type: $errorType, message: $errorMsg, cause: $errorCause", e)
                errorMessage = "Publish error ($errorType): $errorMsg"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateProduct(
        productId: String,
        productName: String,
        description: String,
        priceStr: String,
        category: String,
        condition: String,
        location: String,
        exchangeAvailable: Boolean,
        onSuccess: () -> Unit
    ) {
        if (productName.isBlank() || description.isBlank() || priceStr.toDoubleOrNull() == null || category.isBlank() || condition.isBlank() || location.isBlank()) {
            errorMessage = "Please fill in all required fields correctly."
            return
        }
        val price = priceStr.toDoubleOrNull() ?: 0.0

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "productName" to productName.trim(),
                    "description" to description.trim(),
                    "price" to price,
                    "category" to category,
                    "condition" to condition,
                    "location" to location.trim(),
                    "exchangeAvailable" to exchangeAvailable
                )
                val result = repository.updateProduct(productId, updates)
                result.fold(
                    onSuccess = {
                        successMessage = "Product updated successfully"
                        onSuccess()
                    },
                    onFailure = { err ->
                        errorMessage = err.localizedMessage ?: "Failed to update product"
                    }
                )
            } catch (e: Exception) {
                errorMessage = "Update error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteProduct(productId)
                result.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Failed to delete product"
                }
            } catch (e: Exception) {
                errorMessage = "Delete error: ${e.localizedMessage}"
            }
        }
    }

    fun markAsSold(productId: String) {
        viewModelScope.launch {
            try {
                val result = repository.markAsSold(productId)
                result.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Failed to mark product as sold"
                }
            } catch (e: Exception) {
                errorMessage = "Mark as sold error: ${e.localizedMessage}"
            }
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
