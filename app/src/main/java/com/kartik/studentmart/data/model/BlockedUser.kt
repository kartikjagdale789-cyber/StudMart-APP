package com.kartik.studentmart.data.model

data class BlockedUser(
    val userId: String = "",
    val blockedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "blockedAt" to blockedAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>, id: String): BlockedUser {
            return BlockedUser(
                userId = map["userId"] as? String ?: id,
                blockedAt = (map["blockedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
