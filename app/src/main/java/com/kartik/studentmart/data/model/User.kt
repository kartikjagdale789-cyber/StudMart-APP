package com.kartik.studentmart.data.model

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val role: String = "student"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "profileImageUrl" to profileImageUrl,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "role" to role
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): User {
            return User(
                userId = map["userId"] as? String ?: id,
                fullName = map["fullName"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phoneNumber = map["phoneNumber"] as? String ?: "",
                profileImageUrl = map["profileImageUrl"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                role = map["role"] as? String ?: "student"
            )
        }
    }
}
