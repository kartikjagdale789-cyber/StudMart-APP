package com.kartik.studentmart.data.model

data class Chat(
    val chatId: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val productPrice: Double = 0.0,
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val lastMessage: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "chatId" to chatId,
            "productId" to productId,
            "productName" to productName,
            "productImage" to productImage,
            "productPrice" to productPrice,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "lastMessage" to lastMessage,
            "status" to status,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): Chat {
            return Chat(
                chatId = map["chatId"] as? String ?: id,
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                productImage = map["productImage"] as? String ?: "",
                productPrice = (map["productPrice"] as? Number)?.toDouble() ?: 0.0,
                buyerId = map["buyerId"] as? String ?: "",
                buyerName = map["buyerName"] as? String ?: "",
                sellerId = map["sellerId"] as? String ?: "",
                sellerName = map["sellerName"] as? String ?: "",
                lastMessage = map["lastMessage"] as? String ?: "",
                status = map["status"] as? String ?: "ACTIVE",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = "text", // "text" or "offer"
    val offerId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "messageId" to messageId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "type" to type,
            "offerId" to offerId,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): ChatMessage {
            return ChatMessage(
                messageId = map["messageId"] as? String ?: id,
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                text = map["text"] as? String ?: "",
                type = map["type"] as? String ?: "text",
                offerId = map["offerId"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
