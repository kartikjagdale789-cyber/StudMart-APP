package com.kartik.studentmart.data.model

data class Offer(
    val offerId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val listedPrice: Double = 0.0,
    val offerPrice: Double = 0.0,
    val message: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED
    val type: String = "SALE", // SALE, EXCHANGE
    val offeredProductId: String = "",
    val offeredProductName: String = "",
    val offeredProductImage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "offerId" to offerId,
            "productId" to productId,
            "productName" to productName,
            "productImage" to productImage,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "listedPrice" to listedPrice,
            "offerPrice" to offerPrice,
            "message" to message,
            "status" to status,
            "type" to type,
            "offeredProductId" to offeredProductId,
            "offeredProductName" to offeredProductName,
            "offeredProductImage" to offeredProductImage,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): Offer {
            return Offer(
                offerId = map["offerId"] as? String ?: id,
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                productImage = map["productImage"] as? String ?: "",
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "",
                buyerId = map["buyerId"] as? String ?: "",
                buyerName = map["buyerName"] as? String ?: "",
                listedPrice = (map["listedPrice"] as? Number)?.toDouble() ?: 0.0,
                offerPrice = (map["offerPrice"] as? Number)?.toDouble() ?: 0.0,
                message = map["message"] as? String ?: "",
                status = map["status"] as? String ?: "PENDING",
                type = map["type"] as? String ?: "SALE",
                offeredProductId = map["offeredProductId"] as? String ?: "",
                offeredProductName = map["offeredProductName"] as? String ?: "",
                offeredProductImage = map["offeredProductImage"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
