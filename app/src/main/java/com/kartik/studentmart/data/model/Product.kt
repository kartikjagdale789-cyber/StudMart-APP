package com.kartik.studentmart.data.model

data class Product(
    val productId: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productName: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE", // ACTIVE, SOLD
    val exchangeAvailable: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        val validExchange = if (category.equals("Books", ignoreCase = true)) exchangeAvailable else false
        return mapOf(
            "productId" to productId,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "productName" to productName,
            "description" to description,
            "price" to price,
            "category" to category,
            "condition" to condition,
            "location" to location,
            "imageUrls" to imageUrls,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "status" to status,
            "exchangeAvailable" to validExchange
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): Product {
            val cat = map["category"] as? String ?: ""
            val rawExchange = map["exchangeAvailable"] as? Boolean ?: false
            val validExchange = if (cat.equals("Books", ignoreCase = true)) rawExchange else false
            return Product(
                productId = map["productId"] as? String ?: id,
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                description = map["description"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                category = cat,
                condition = map["condition"] as? String ?: "",
                location = map["location"] as? String ?: "",
                imageUrls = (map["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                status = map["status"] as? String ?: "ACTIVE",
                exchangeAvailable = validExchange
            )
        }
    }
}
