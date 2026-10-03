package com.kartik.studentmart.data.model

data class ReportItem(
    val reportId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedUserId: String = "",
    val productId: String = "",
    val productName: String = "",
    val reason: String = "",
    val description: String = "",
    val type: String = "PRODUCT", // PRODUCT or USER
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "reportId" to reportId,
            "reporterId" to reporterId,
            "reporterName" to reporterName,
            "reportedUserId" to reportedUserId,
            "productId" to productId,
            "productName" to productName,
            "reason" to reason,
            "description" to description,
            "type" to type,
            "status" to status,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): ReportItem {
            return ReportItem(
                reportId = map["reportId"] as? String ?: id,
                reporterId = map["reporterId"] as? String ?: "",
                reporterName = map["reporterName"] as? String ?: "",
                reportedUserId = map["reportedUserId"] as? String ?: "",
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                reason = map["reason"] as? String ?: "",
                description = map["description"] as? String ?: "",
                type = map["type"] as? String ?: "PRODUCT",
                status = map["status"] as? String ?: "PENDING",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
