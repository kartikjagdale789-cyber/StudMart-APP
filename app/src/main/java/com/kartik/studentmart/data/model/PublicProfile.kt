package com.kartik.studentmart.data.model

data class PublicProfile(
    val userId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "profileImageUrl" to profileImageUrl,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): PublicProfile {
            return PublicProfile(
                userId = map["userId"] as? String ?: id,
                fullName = map["fullName"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                profileImageUrl = map["profileImageUrl"] as? String ?: "",
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
