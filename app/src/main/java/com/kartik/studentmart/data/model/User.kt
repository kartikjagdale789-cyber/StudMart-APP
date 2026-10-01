package com.kartik.studentmart.data.model

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val role: String = "student"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "fullName" to fullName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "createdAt" to createdAt,
            "role" to role
        )
    }
}
