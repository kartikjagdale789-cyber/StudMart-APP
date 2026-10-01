package com.kartik.studentmart.data.model

data class PurchaseRequest(
    val requestId: String = "",
    val productId: String = "",
    val productName: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val price: Double = 0.0,
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "requestId" to requestId,
            "productId" to productId,
            "productName" to productName,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "price" to price,
            "status" to status,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): PurchaseRequest {
            return PurchaseRequest(
                requestId = map["requestId"] as? String ?: id,
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                buyerId = map["buyerId"] as? String ?: "",
                buyerName = map["buyerName"] as? String ?: "",
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                status = map["status"] as? String ?: "PENDING",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
