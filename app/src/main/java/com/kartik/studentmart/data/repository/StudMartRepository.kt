package com.kartik.studentmart.data.repository

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kartik.studentmart.data.model.BlockedUser
import com.kartik.studentmart.data.model.Chat
import com.kartik.studentmart.data.model.ChatMessage
import com.kartik.studentmart.data.model.NotificationItem
import com.kartik.studentmart.data.model.Offer
import com.kartik.studentmart.data.model.Product
import com.kartik.studentmart.data.model.PublicProfile
import com.kartik.studentmart.data.model.PurchaseRequest
import com.kartik.studentmart.data.model.ReportItem
import com.kartik.studentmart.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StudMartRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    val currentUserId get() = auth.currentUser?.uid
    val currentUserName get() = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "Student"

    // Safety: Reports & Blocking
    suspend fun submitReport(report: ReportItem): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val uid = currentUserId ?: return@withContext Result.failure(Exception("Login required."))
                val ref = firestore.collection("reports").document()
                val finalReport = report.copy(
                    reportId = ref.id,
                    reporterId = uid,
                    reporterName = currentUserName,
                    createdAt = System.currentTimeMillis()
                )
                ref.set(finalReport.toMap()).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getAllReportsFlow(): Flow<List<ReportItem>> = callbackFlow {
        val listener = firestore.collection("reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StudMartAdmin", "Error fetching reports: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { ReportItem.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("reports").document(reportId).update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun blockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (currentUserId == blockedUserId) {
                    return@withContext Result.failure(Exception("You cannot block yourself."))
                }
                val ref = firestore.collection("users").document(currentUserId).collection("blockedUsers").document(blockedUserId)
                val blocked = BlockedUser(userId = blockedUserId, blockedAt = System.currentTimeMillis())
                ref.set(blocked.toMap()).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun unblockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(currentUserId).collection("blockedUsers").document(blockedUserId).delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getBlockedUserIdsFlow(currentUserId: String): Flow<Set<String>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(currentUserId).collection("blockedUsers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.getString("userId") ?: it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    suspend fun isBlockedBetween(userId1: String, userId2: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (userId1.isBlank() || userId2.isBlank()) return@withContext false
                val doc1 = firestore.collection("users").document(userId1).collection("blockedUsers").document(userId2).get().await()
                val doc2 = firestore.collection("users").document(userId2).collection("blockedUsers").document(userId1).get().await()
                doc1.exists() || doc2.exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    // User Profile (Private to current user)
    fun getUserProfileFlow(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val docRef = firestore.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StudMartProfile", "Error fetching profile for $userId: ${error.message}", error)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                val isSelf = userId == currentUserId
                val authUser = if (isSelf) auth.currentUser else null
                val fallbackName = authUser?.displayName?.ifBlank { null }
                    ?: authUser?.email?.substringBefore("@")
                    ?: "Student"
                val fallback = User(
                    userId = userId,
                    fullName = fallbackName,
                    email = authUser?.email ?: "",
                    phoneNumber = "",
                    profileImageUrl = authUser?.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    role = "student"
                )
                if (isSelf && authUser != null) {
                    try {
                        docRef.set(fallback.toMap(), SetOptions.merge())
                        val publicFallback = PublicProfile(
                            userId = userId,
                            fullName = fallbackName,
                            phoneNumber = "",
                            profileImageUrl = authUser.photoUrl?.toString() ?: "",
                            updatedAt = System.currentTimeMillis()
                        )
                        firestore.collection("publicProfiles").document(userId).set(publicFallback.toMap(), SetOptions.merge())
                    } catch (e: Exception) {
                        Log.e("StudMartProfile", "Failed to auto-create user profile", e)
                    }
                }
                trySend(fallback)
            } else {
                val data = snapshot.data
                if (data != null) {
                    trySend(User.fromMap(data, snapshot.id))
                } else {
                    trySend(null)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    // Public Profile (Read by marketplace buyers)
    fun getPublicProfileFlow(sellerId: String): Flow<PublicProfile?> = callbackFlow {
        if (sellerId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val docRef = firestore.collection("publicProfiles").document(sellerId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StudMartPublicProfile", "Error fetching public profile for $sellerId: ${error.message}", error)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                val profile = PublicProfile.fromMap(snapshot.data!!, snapshot.id)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(userId: String, fullName: String, phoneNumber: String, profileImageUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userDocRef = firestore.collection("users").document(userId)
                val publicDocRef = firestore.collection("publicProfiles").document(userId)

                val userDoc = userDocRef.get().await()
                val userUpdates = mutableMapOf<String, Any>(
                    "fullName" to fullName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "updatedAt" to System.currentTimeMillis()
                )
                val publicUpdates = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "fullName" to fullName.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "updatedAt" to System.currentTimeMillis()
                )

                if (profileImageUrl.isNotBlank()) {
                    userUpdates["profileImageUrl"] = profileImageUrl
                    publicUpdates["profileImageUrl"] = profileImageUrl
                }

                if (userDoc.exists()) {
                    userDocRef.update(userUpdates).await()
                } else {
                    val authUser = auth.currentUser
                    val newDoc = User(
                        userId = userId,
                        fullName = fullName.trim(),
                        email = authUser?.email ?: "",
                        phoneNumber = phoneNumber.trim(),
                        profileImageUrl = profileImageUrl,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        role = "student"
                    )
                    userDocRef.set(newDoc.toMap()).await()
                }

                publicDocRef.set(publicUpdates, SetOptions.merge()).await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("StudMartProfile", "Failed to update profile", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = auth.currentUser ?: return@withContext Result.failure(Exception("No user logged in."))
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteUserAccount(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = auth.currentUser ?: return@withContext Result.failure(Exception("No user logged in."))
                val uid = user.uid

                try {
                    firestore.collection("users").document(uid).delete().await()
                    firestore.collection("publicProfiles").document(uid).delete().await()
                } catch (e: Exception) {
                    Log.e("StudMartDelete", "Failed to delete user profile", e)
                }

                user.delete().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Notifications
    suspend fun createNotification(
        recipientId: String,
        senderId: String,
        senderName: String,
        type: String,
        title: String,
        message: String,
        productId: String = "",
        productName: String = "",
        offerId: String = "",
        chatId: String = ""
    ) {
        try {
            if (recipientId.isBlank() || recipientId == senderId) return
            val notifRef = firestore.collection("notifications").document()
            val notif = NotificationItem(
                notificationId = notifRef.id,
                recipientId = recipientId,
                senderId = senderId,
                senderName = senderName,
                type = type,
                title = title,
                message = message,
                productId = productId,
                productName = productName,
                offerId = offerId,
                chatId = chatId,
                isRead = false,
                createdAt = System.currentTimeMillis()
            )
            notifRef.set(notif.toMap()).await()
        } catch (e: Exception) {
            Log.e("StudMartNotif", "Failed to create notification", e)
        }
    }

    fun getNotificationsFlow(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { NotificationItem.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun getUnreadNotificationCountFlow(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val unread = firestore.collection("notifications")
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("isRead", false)
                    .get().await()

                for (doc in unread.documents) {
                    doc.reference.update("isRead", true)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all ACTIVE products for Home (newest first)
    fun getActiveProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("products")
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Product.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    // Get products belonging to current user (My Listings)
    fun getUserProductsFlow(userId: String): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("products")
            .whereEqualTo("sellerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Product.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProductById(productId: String): Result<Product?> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = firestore.collection("products").document(productId).get().await()
                val product = doc.data?.let { Product.fromMap(it, doc.id) }
                Result.success(product)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Wishlist Flows
    fun getWishlistProductIdsFlow(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId).collection("wishlist")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.getString("productId") ?: it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    fun getWishlistProductsFlow(userId: String): Flow<List<Product>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId).collection("wishlist")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val productIds = snapshot?.documents?.mapNotNull { it.getString("productId") ?: it.id } ?: emptyList()
                if (productIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tasks = productIds.map { id ->
                    firestore.collection("products").document(id).get()
                }
                Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                    .addOnSuccessListener { docs ->
                        val products = docs.mapNotNull { doc ->
                            if (doc.exists() && doc.data != null) {
                                Product.fromMap(doc.data!!, doc.id)
                            } else null
                        }
                        trySend(products)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addToWishlist(userId: String, productId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("wishlist").document(productId)
            docRef.set(mapOf(
                "productId" to productId,
                "addedAt" to System.currentTimeMillis()
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(userId: String, productId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("wishlist").document(productId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Purchase Requests
    suspend fun createPurchaseRequest(product: Product): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val buyerId = currentUserId ?: return@withContext Result.failure(Exception("Login required to submit a purchase request."))
                val buyerName = currentUserName

                if (buyerId == product.sellerId) {
                    return@withContext Result.failure(Exception("You cannot purchase your own product."))
                }

                if (isBlockedBetween(buyerId, product.sellerId)) {
                    return@withContext Result.failure(Exception("You can't purchase because one of you has blocked the other."))
                }

                val productDoc = firestore.collection("products").document(product.productId).get().await()
                val latestStatus = productDoc.getString("status") ?: "ACTIVE"
                if (latestStatus == "SOLD") {
                    return@withContext Result.failure(Exception("Product is no longer available."))
                }

                val existingRequests = firestore.collection("purchaseRequests")
                    .whereEqualTo("buyerId", buyerId)
                    .whereEqualTo("productId", product.productId)
                    .get().await()

                val hasActiveRequest = existingRequests.documents.any { doc ->
                    val status = doc.getString("status") ?: ""
                    status == "PENDING" || status == "ACCEPTED"
                }

                if (hasActiveRequest) {
                    return@withContext Result.failure(Exception("You already have an active purchase request for this product."))
                }

                val reqRef = firestore.collection("purchaseRequests").document()
                val request = PurchaseRequest(
                    requestId = reqRef.id,
                    productId = product.productId,
                    productName = product.productName,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    sellerId = product.sellerId,
                    sellerName = product.sellerName,
                    price = product.price,
                    status = "PENDING",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                reqRef.set(request.toMap()).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getBuyerPurchaseRequestsFlow(buyerId: String): Flow<List<PurchaseRequest>> = callbackFlow {
        if (buyerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("purchaseRequests")
            .whereEqualTo("buyerId", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { PurchaseRequest.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    fun getSellerPurchaseRequestsFlow(sellerId: String): Flow<List<PurchaseRequest>> = callbackFlow {
        if (sellerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("purchaseRequests")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { PurchaseRequest.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancelPurchaseRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("purchaseRequests").document(requestId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptPurchaseRequest(requestId: String, productId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("purchaseRequests").document(requestId).update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                firestore.collection("products").document(productId).update(
                    mapOf(
                        "status" to "SOLD",
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                val pendingRequests = firestore.collection("purchaseRequests")
                    .whereEqualTo("productId", productId)
                    .whereEqualTo("status", "PENDING")
                    .get().await()

                for (doc in pendingRequests.documents) {
                    if (doc.id != requestId) {
                        doc.reference.update(
                            mapOf(
                                "status" to "REJECTED",
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun rejectPurchaseRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("purchaseRequests").document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Offers
    suspend fun createOffer(product: Product, offerPrice: Double, message: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val buyerId = currentUserId ?: return@withContext Result.failure(Exception("Login required to make an offer."))
                val buyerName = currentUserName

                if (buyerId == product.sellerId) {
                    return@withContext Result.failure(Exception("You cannot make an offer on your own product."))
                }

                if (isBlockedBetween(buyerId, product.sellerId)) {
                    return@withContext Result.failure(Exception("You can't make an offer to this user because one of you has blocked the other."))
                }

                val productDoc = firestore.collection("products").document(product.productId).get().await()
                val latestStatus = productDoc.getString("status") ?: "ACTIVE"
                if (latestStatus == "SOLD") {
                    return@withContext Result.failure(Exception("Product is no longer available."))
                }

                val existingOffers = firestore.collection("offers")
                    .whereEqualTo("buyerId", buyerId)
                    .whereEqualTo("productId", product.productId)
                    .get().await()

                val hasActiveOffer = existingOffers.documents.any { doc ->
                    val status = doc.getString("status") ?: ""
                    status == "PENDING" || status == "ACCEPTED"
                }

                if (hasActiveOffer) {
                    return@withContext Result.failure(Exception("You already have an active offer for this product."))
                }

                val offerRef = firestore.collection("offers").document()
                val offer = Offer(
                    offerId = offerRef.id,
                    productId = product.productId,
                    productName = product.productName,
                    productImage = product.imageUrls.firstOrNull() ?: "",
                    sellerId = product.sellerId,
                    sellerName = product.sellerName,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    listedPrice = product.price,
                    offerPrice = offerPrice,
                    message = message.trim(),
                    status = "PENDING",
                    type = "SALE",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                offerRef.set(offer.toMap()).await()

                // Automatically link offer to Chat
                var chatIdVal = ""
                val chatResult = getOrCreateChat(product)
                if (chatResult.isSuccess) {
                    chatIdVal = chatResult.getOrThrow()
                    val msgRef = firestore.collection("chats").document(chatIdVal).collection("messages").document()
                    val chatMsg = ChatMessage(
                        messageId = msgRef.id,
                        senderId = buyerId,
                        senderName = buyerName,
                        text = if (message.isNotBlank()) "Sale Offer: ₹$offerPrice (\"${message.trim()}\")" else "Sale Offer: ₹$offerPrice",
                        type = "offer",
                        offerId = offerRef.id,
                        createdAt = System.currentTimeMillis()
                    )
                    msgRef.set(chatMsg.toMap()).await()
                    firestore.collection("chats").document(chatIdVal).update(
                        mapOf(
                            "lastMessage" to "Offer: ₹$offerPrice",
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                }

                // NOTIFICATION: NEW_OFFER to seller
                createNotification(
                    recipientId = product.sellerId,
                    senderId = buyerId,
                    senderName = buyerName,
                    type = "NEW_OFFER",
                    title = "New Offer Received",
                    message = "$buyerName offered ₹$offerPrice for your ${product.productName}.",
                    productId = product.productId,
                    productName = product.productName,
                    offerId = offerRef.id,
                    chatId = chatIdVal
                )

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createExchangeOffer(requestedProduct: Product, offeredProduct: Product, message: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val buyerId = currentUserId ?: return@withContext Result.failure(Exception("Login required to make an exchange offer."))
                val buyerName = currentUserName

                if (buyerId == requestedProduct.sellerId) {
                    return@withContext Result.failure(Exception("You cannot exchange with yourself."))
                }
                if (offeredProduct.sellerId != buyerId) {
                    return@withContext Result.failure(Exception("You can only offer a product that belongs to you."))
                }
                if (offeredProduct.productId == requestedProduct.productId) {
                    return@withContext Result.failure(Exception("You cannot exchange a product for itself."))
                }

                if (isBlockedBetween(buyerId, requestedProduct.sellerId)) {
                    return@withContext Result.failure(Exception("You can't exchange with this user because one of you has blocked the other."))
                }

                // 1. Verify requested product is a Book and exchange is allowed
                if (!requestedProduct.category.equals("Books", ignoreCase = true) || !requestedProduct.exchangeAvailable) {
                    return@withContext Result.failure(Exception("Exchange offers are allowed only for books with exchange available."))
                }

                // 2. Verify offered product is a Book
                if (!offeredProduct.category.equals("Books", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("You can only offer books for an exchange."))
                }

                // 3. Verify both products are ACTIVE in Firestore
                val reqDoc = firestore.collection("products").document(requestedProduct.productId).get().await()
                val offDoc = firestore.collection("products").document(offeredProduct.productId).get().await()
                if (reqDoc.getString("status") == "SOLD" || offDoc.getString("status") == "SOLD") {
                    return@withContext Result.failure(Exception("One of the products is no longer available."))
                }

                // 4. Check duplicate active exchange offer
                val existingOffers = firestore.collection("offers")
                    .whereEqualTo("buyerId", buyerId)
                    .whereEqualTo("productId", requestedProduct.productId)
                    .get().await()

                val hasActiveExchange = existingOffers.documents.any { doc ->
                    val status = doc.getString("status") ?: ""
                    val type = doc.getString("type") ?: ""
                    type == "EXCHANGE" && (status == "PENDING" || status == "ACCEPTED")
                }

                if (hasActiveExchange) {
                    return@withContext Result.failure(Exception("You already have an active exchange offer for this product."))
                }

                val offerRef = firestore.collection("offers").document()
                val offer = Offer(
                    offerId = offerRef.id,
                    productId = requestedProduct.productId,
                    productName = requestedProduct.productName,
                    productImage = requestedProduct.imageUrls.firstOrNull() ?: "",
                    sellerId = requestedProduct.sellerId,
                    sellerName = requestedProduct.sellerName,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    listedPrice = requestedProduct.price,
                    offerPrice = offeredProduct.price,
                    message = message.trim(),
                    status = "PENDING",
                    type = "EXCHANGE",
                    offeredProductId = offeredProduct.productId,
                    offeredProductName = offeredProduct.productName,
                    offeredProductImage = offeredProduct.imageUrls.firstOrNull() ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                offerRef.set(offer.toMap()).await()

                // Link Exchange Offer to Chat
                var chatIdVal = ""
                val chatResult = getOrCreateChat(requestedProduct)
                if (chatResult.isSuccess) {
                    chatIdVal = chatResult.getOrThrow()
                    val msgRef = firestore.collection("chats").document(chatIdVal).collection("messages").document()
                    val chatMsg = ChatMessage(
                        messageId = msgRef.id,
                        senderId = buyerId,
                        senderName = buyerName,
                        text = "Exchange Offer: '${offeredProduct.productName}' for '${requestedProduct.productName}'",
                        type = "offer",
                        offerId = offerRef.id,
                        createdAt = System.currentTimeMillis()
                    )
                    msgRef.set(chatMsg.toMap()).await()
                    firestore.collection("chats").document(chatIdVal).update(
                        mapOf(
                            "lastMessage" to "Exchange Offer: ${offeredProduct.productName}",
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                }

                // NOTIFICATION: NEW_EXCHANGE_OFFER to seller
                createNotification(
                    recipientId = requestedProduct.sellerId,
                    senderId = buyerId,
                    senderName = buyerName,
                    type = "NEW_EXCHANGE_OFFER",
                    title = "New Exchange Offer",
                    message = "$buyerName wants to exchange ${offeredProduct.productName} for your ${requestedProduct.productName}.",
                    productId = requestedProduct.productId,
                    productName = requestedProduct.productName,
                    offerId = offerRef.id,
                    chatId = chatIdVal
                )

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getOfferByIdFlow(offerId: String): Flow<Offer?> = callbackFlow {
        if (offerId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("offers").document(offerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot.data?.let { Offer.fromMap(it, snapshot.id) })
            }
        awaitClose { listener.remove() }
    }

    fun getBuyerOffersFlow(buyerId: String): Flow<List<Offer>> = callbackFlow {
        if (buyerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("offers")
            .whereEqualTo("buyerId", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Offer.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(offers)
            }
        awaitClose { listener.remove() }
    }

    fun getSellerOffersFlow(sellerId: String): Flow<List<Offer>> = callbackFlow {
        if (sellerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("offers")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val offers = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Offer.fromMap(it, doc.id) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(offers)
            }
        awaitClose { listener.remove() }
    }

    suspend fun cancelOffer(offerId: String): Result<Unit> {
        return try {
            firestore.collection("offers").document(offerId).update(
                mapOf(
                    "status" to "CANCELLED",
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptOffer(offerId: String, productId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val productDoc = firestore.collection("products").document(productId).get().await()
                val latestStatus = productDoc.getString("status") ?: "ACTIVE"
                if (latestStatus != "ACTIVE") {
                    return@withContext Result.failure(Exception("Product is no longer active."))
                }

                val offerDoc = firestore.collection("offers").document(offerId).get().await()
                val offerType = offerDoc.getString("type") ?: "SALE"
                val buyerId = offerDoc.getString("buyerId") ?: ""
                val productName = offerDoc.getString("productName") ?: productDoc.getString("productName") ?: ""
                val offerPrice = offerDoc.getDouble("offerPrice") ?: 0.0

                if (offerType == "EXCHANGE") {
                    val offeredProductId = offerDoc.getString("offeredProductId") ?: ""
                    if (offeredProductId.isNotBlank()) {
                        val offeredDoc = firestore.collection("products").document(offeredProductId).get().await()
                        if (offeredDoc.getString("status") == "SOLD") {
                            return@withContext Result.failure(Exception("Offered exchange product is no longer active."))
                        }

                        // Mark both products as SOLD for Exchange Acceptance
                        firestore.collection("products").document(productId).update(
                            mapOf("status" to "SOLD", "updatedAt" to System.currentTimeMillis())
                        ).await()
                        firestore.collection("products").document(offeredProductId).update(
                            mapOf("status" to "SOLD", "updatedAt" to System.currentTimeMillis())
                        ).await()
                    }
                }

                firestore.collection("offers").document(offerId).update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                // NOTIFICATION: OFFER_ACCEPTED or EXCHANGE_ACCEPTED
                if (buyerId.isNotBlank()) {
                    val notifType = if (offerType == "EXCHANGE") "EXCHANGE_ACCEPTED" else "OFFER_ACCEPTED"
                    val notifTitle = if (offerType == "EXCHANGE") "Exchange Offer Accepted" else "Offer Accepted"
                    val notifMsg = if (offerType == "EXCHANGE") "Your exchange offer for $productName was accepted." else "Your offer of ₹$offerPrice for $productName was accepted."
                    createNotification(
                        recipientId = buyerId,
                        senderId = currentUserId ?: "",
                        senderName = currentUserName,
                        type = notifType,
                        title = notifTitle,
                        message = notifMsg,
                        productId = productId,
                        productName = productName,
                        offerId = offerId
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun acceptExchangeOffer(offerId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val offerDoc = firestore.collection("offers").document(offerId).get().await()
                if (!offerDoc.exists()) {
                    return@withContext Result.failure(Exception("Offer not found."))
                }
                val productId = offerDoc.getString("productId") ?: ""
                val offeredProductId = offerDoc.getString("offeredProductId") ?: ""
                val buyerId = offerDoc.getString("buyerId") ?: ""
                val productName = offerDoc.getString("productName") ?: ""

                val reqDoc = firestore.collection("products").document(productId).get().await()
                val offDoc = firestore.collection("products").document(offeredProductId).get().await()

                if (reqDoc.getString("status") == "SOLD" || offDoc.getString("status") == "SOLD") {
                    return@withContext Result.failure(Exception("One of the products is no longer active."))
                }

                // Batch update: set offer ACCEPTED, set both products SOLD
                firestore.collection("offers").document(offerId).update(
                    mapOf("status" to "ACCEPTED", "updatedAt" to System.currentTimeMillis())
                ).await()

                firestore.collection("products").document(productId).update(
                    mapOf("status" to "SOLD", "updatedAt" to System.currentTimeMillis())
                ).await()

                if (offeredProductId.isNotBlank()) {
                    firestore.collection("products").document(offeredProductId).update(
                        mapOf("status" to "SOLD", "updatedAt" to System.currentTimeMillis())
                    ).await()
                }

                // NOTIFICATION: EXCHANGE_ACCEPTED
                if (buyerId.isNotBlank()) {
                    createNotification(
                        recipientId = buyerId,
                        senderId = currentUserId ?: "",
                        senderName = currentUserName,
                        type = "EXCHANGE_ACCEPTED",
                        title = "Exchange Offer Accepted",
                        message = "Your exchange offer for $productName was accepted.",
                        productId = productId,
                        productName = productName,
                        offerId = offerId
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun rejectOffer(offerId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val offerDoc = firestore.collection("offers").document(offerId).get().await()
                val buyerId = offerDoc.getString("buyerId") ?: ""
                val productName = offerDoc.getString("productName") ?: ""
                val offerType = offerDoc.getString("type") ?: "SALE"

                firestore.collection("offers").document(offerId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                // NOTIFICATION: OFFER_REJECTED or EXCHANGE_REJECTED
                if (buyerId.isNotBlank()) {
                    val notifType = if (offerType == "EXCHANGE") "EXCHANGE_REJECTED" else "OFFER_REJECTED"
                    val notifTitle = if (offerType == "EXCHANGE") "Exchange Offer Rejected" else "Offer Rejected"
                    val notifMsg = if (offerType == "EXCHANGE") "Your exchange offer for $productName was rejected." else "Your offer for $productName was rejected."
                    createNotification(
                        recipientId = buyerId,
                        senderId = currentUserId ?: "",
                        senderName = currentUserName,
                        type = notifType,
                        title = notifTitle,
                        message = notifMsg,
                        productId = offerDoc.getString("productId") ?: "",
                        productName = productName,
                        offerId = offerId
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Chats
    suspend fun getOrCreateChat(product: Product): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val buyerId = currentUserId ?: return@withContext Result.failure(Exception("Login required to start a chat."))
                if (buyerId == product.sellerId) {
                    return@withContext Result.failure(Exception("You cannot chat with yourself."))
                }

                if (isBlockedBetween(buyerId, product.sellerId)) {
                    return@withContext Result.failure(Exception("You can't chat with this user because one of you has blocked the other."))
                }

                val productDoc = firestore.collection("products").document(product.productId).get().await()
                val latestStatus = productDoc.getString("status") ?: "ACTIVE"
                if (latestStatus == "SOLD") {
                    return@withContext Result.failure(Exception("This product has been sold."))
                }

                val existingChats = firestore.collection("chats")
                    .whereEqualTo("productId", product.productId)
                    .whereEqualTo("buyerId", buyerId)
                    .whereEqualTo("sellerId", product.sellerId)
                    .get().await()

                val activeChat = existingChats.documents.firstOrNull()
                if (activeChat != null) {
                    return@withContext Result.success(activeChat.id)
                }

                val chatRef = firestore.collection("chats").document()
                val chat = Chat(
                    chatId = chatRef.id,
                    productId = product.productId,
                    productName = product.productName,
                    productImage = product.imageUrls.firstOrNull() ?: "",
                    productPrice = product.price,
                    buyerId = buyerId,
                    buyerName = currentUserName,
                    sellerId = product.sellerId,
                    sellerName = product.sellerName,
                    lastMessage = "Chat started",
                    status = "ACTIVE",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                chatRef.set(chat.toMap()).await()
                Result.success(chatRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getChatByIdFlow(chatId: String): Flow<Chat?> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("chats").document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot.data?.let { Chat.fromMap(it, snapshot.id) })
            }
        awaitClose { listener.remove() }
    }

    fun getChatMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val listener = firestore.collection("chats").document(chatId).collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { ChatMessage.fromMap(it, doc.id) }
                }?.sortedBy { it.createdAt } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun getUserChatsFlow(userId: String): Flow<List<Chat>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var buyerChats = emptyList<Chat>()
        var sellerChats = emptyList<Chat>()

        fun emitCombined() {
            val allMap = mutableMapOf<String, Chat>()
            buyerChats.forEach { allMap[it.chatId] = it }
            sellerChats.forEach { allMap[it.chatId] = it }
            trySend(allMap.values.sortedByDescending { it.updatedAt })
        }

        val l1 = firestore.collection("chats").whereEqualTo("buyerId", userId)
            .addSnapshotListener { snapshot, _ ->
                buyerChats = snapshot?.documents?.mapNotNull { doc -> doc.data?.let { Chat.fromMap(it, doc.id) } } ?: emptyList()
                emitCombined()
            }

        val l2 = firestore.collection("chats").whereEqualTo("sellerId", userId)
            .addSnapshotListener { snapshot, _ ->
                sellerChats = snapshot?.documents?.mapNotNull { doc -> doc.data?.let { Chat.fromMap(it, doc.id) } } ?: emptyList()
                emitCombined()
            }

        awaitClose {
            l1.remove()
            l2.remove()
        }
    }

    suspend fun sendMessage(chatId: String, text: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val trimmedText = text.trim()
                if (trimmedText.isBlank()) {
                    return@withContext Result.failure(Exception("Message cannot be empty."))
                }
                val userId = currentUserId ?: return@withContext Result.failure(Exception("Login required."))
                val userName = currentUserName

                val chatDoc = firestore.collection("chats").document(chatId).get().await()
                val buyerId = chatDoc.getString("buyerId") ?: ""
                val sellerId = chatDoc.getString("sellerId") ?: ""
                if (isBlockedBetween(buyerId, sellerId)) {
                    return@withContext Result.failure(Exception("You can't send messages because one of you has blocked the other."))
                }

                val msgRef = firestore.collection("chats").document(chatId).collection("messages").document()
                val message = ChatMessage(
                    messageId = msgRef.id,
                    senderId = userId,
                    senderName = userName,
                    text = trimmedText,
                    createdAt = System.currentTimeMillis()
                )

                msgRef.set(message.toMap()).await()

                firestore.collection("chats").document(chatId).update(
                    mapOf(
                        "lastMessage" to trimmedText,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                val recipientId = if (userId == buyerId) sellerId else buyerId
                if (recipientId.isNotBlank() && recipientId != userId) {
                    createNotification(
                        recipientId = recipientId,
                        senderId = userId,
                        senderName = userName,
                        type = "NEW_MESSAGE",
                        title = "New Message",
                        message = "$userName sent you a message.",
                        productId = chatDoc.getString("productId") ?: "",
                        productName = chatDoc.getString("productName") ?: "",
                        chatId = chatId
                    )
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun uploadImages(uris: List<Uri>): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val context = FirebaseApp.getInstance().applicationContext
                val downloadUrls = mutableListOf<String>()

                for (uri in uris) {
                    Log.d("StudMartCloudinary", "Starting upload for uri: $uri")

                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(Exception("Failed to open input stream for image URI: $uri"))
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload_preset", "studmart_products")
                        .addFormDataPart(
                            "file",
                            "image.jpg",
                            bytes.toRequestBody(mediaType)
                        )
                        .build()

                    val url = "https://api.cloudinary.com/v1_1/iz5wgvde/image/upload"
                    Log.d("StudMartCloudinary", "Request URL: $url")

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBodyStr = response.body?.string() ?: ""

                    Log.d("StudMartCloudinary", "Response Code: ${response.code}, Body: $responseBodyStr")

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Cloudinary upload failed (HTTP ${response.code}): $responseBodyStr"))
                    }

                    val jsonObject = JSONObject(responseBodyStr)
                    val secureUrl = jsonObject.optString("secure_url")
                    if (secureUrl.isBlank()) {
                        return@withContext Result.failure(Exception("Cloudinary response missing secure_url. Full response: $responseBodyStr"))
                    }
                    downloadUrls.add(secureUrl)
                }

                Result.success(downloadUrls)
            } catch (e: Exception) {
                val errorType = e.javaClass.simpleName
                val errorMsg = e.message ?: "Unknown message"
                val errorCause = e.cause?.toString() ?: "None"
                Log.e("StudMartCloudinary", "Upload exception type: $errorType, message: $errorMsg, cause: $errorCause", e)
                Result.failure(Exception("Image upload error ($errorType): $errorMsg", e))
            }
        }
    }

    suspend fun createProduct(product: Product): Result<Unit> {
        return try {
            val docRef = firestore.collection("products").document()
            val finalProduct = product.copy(
                productId = docRef.id,
                sellerId = currentUserId ?: "",
                sellerName = currentUserName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(finalProduct.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(productId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val mutableUpdates = updates.toMutableMap()
            mutableUpdates["updatedAt"] = System.currentTimeMillis()
            
            // Enforce Book exchange rule if category is updated
            if (mutableUpdates.containsKey("category")) {
                val cat = mutableUpdates["category"] as? String ?: ""
                if (!cat.equals("Books", ignoreCase = true)) {
                    mutableUpdates["exchangeAvailable"] = false
                }
            }

            firestore.collection("products").document(productId).update(mutableUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsSold(productId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = currentUserId ?: return@withContext Result.failure(Exception("Login required."))
                
                val productDoc = firestore.collection("products").document(productId).get().await()
                if (!productDoc.exists()) {
                    return@withContext Result.failure(Exception("Product not found."))
                }
                val sellerId = productDoc.getString("sellerId")
                if (sellerId != userId) {
                    return@withContext Result.failure(Exception("Only the product seller can mark it as sold."))
                }

                // 1. Update product status to SOLD
                firestore.collection("products").document(productId).update(
                    mapOf(
                        "status" to "SOLD",
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                // Notify involved buyer if there is an accepted offer
                val acceptedOffers = firestore.collection("offers")
                    .whereEqualTo("productId", productId)
                    .whereEqualTo("status", "ACCEPTED")
                    .get().await()

                for (offerDoc in acceptedOffers.documents) {
                    val buyerId = offerDoc.getString("buyerId") ?: ""
                    val productName = offerDoc.getString("productName") ?: productDoc.getString("productName") ?: ""
                    if (buyerId.isNotBlank()) {
                        createNotification(
                            recipientId = buyerId,
                            senderId = userId,
                            senderName = currentUserName,
                            type = "PRODUCT_SOLD",
                            title = "Product Sold",
                            message = "$productName has been marked as sold.",
                            productId = productId,
                            productName = productName,
                            offerId = offerDoc.id
                        )
                    }
                }

                // 2. Close related active/pending offers for this product
                val pendingOffers = firestore.collection("offers")
                    .whereEqualTo("productId", productId)
                    .whereEqualTo("status", "PENDING")
                    .get().await()

                for (doc in pendingOffers.documents) {
                    doc.reference.update(
                        mapOf(
                            "status" to "CANCELLED",
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }

                // 3. Find and delete related chats & messages for this product
                val relatedChats = firestore.collection("chats")
                    .whereEqualTo("productId", productId)
                    .get().await()

                for (chatDoc in relatedChats.documents) {
                    val messagesSnapshot = chatDoc.reference.collection("messages").get().await()
                    for (msgDoc in messagesSnapshot.documents) {
                        msgDoc.reference.delete()
                    }
                    chatDoc.reference.delete()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
