package com.kartik.studentmart.data.model

data class NotificationItem(
    val notificationId: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "", // NEW_OFFER, OFFER_ACCEPTED, OFFER_REJECTED, NEW_EXCHANGE_OFFER, EXCHANGE_ACCEPTED, EXCHANGE_REJECTED, NEW_MESSAGE, PRODUCT_SOLD
    val title: String = "",
    val message: String = "",
    val productId: String = "",
    val productName: String = "",
    val offerId: String = "",
    val chatId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "notificationId" to notificationId,
            "recipientId" to recipientId,
            "senderId" to senderId,
            "senderName" to senderName,
            "type" to type,
            "title" to title,
            "message" to message,
            "productId" to productId,
            "productName" to productName,
            "offerId" to offerId,
            "chatId" to chatId,
            "isRead" to isRead,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): NotificationItem {
            return NotificationItem(
                notificationId = map["notificationId"] as? String ?: id,
                recipientId = map["recipientId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                type = map["type"] as? String ?: "",
                title = map["title"] as? String ?: "",
                message = map["message"] as? String ?: "",
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                offerId = map["offerId"] as? String ?: "",
                chatId = map["chatId"] as? String ?: "",
                isRead = map["isRead"] as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
