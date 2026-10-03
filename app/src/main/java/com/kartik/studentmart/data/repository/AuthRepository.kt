package com.kartik.studentmart.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kartik.studentmart.data.model.PublicProfile
import com.kartik.studentmart.data.model.User
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        pass: String
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID is null")
            
            val user = User(
                userId = userId,
                fullName = fullName,
                email = email,
                phoneNumber = phone
            )

            val publicProfile = PublicProfile(
                userId = userId,
                fullName = fullName,
                phoneNumber = phone,
                profileImageUrl = "",
                updatedAt = System.currentTimeMillis()
            )

            try {
                withTimeout(10000L) {
                    firestore.collection("users").document(userId).set(user.toMap()).await()
                    firestore.collection("publicProfiles").document(userId).set(publicProfile.toMap()).await()
                }
            } catch (e: TimeoutCancellationException) {
                return Result.failure(Exception("Cloud Firestore request timed out. Please ensure Cloud Firestore is enabled in your Firebase Console."))
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to save user profile to Firestore: ${e.localizedMessage}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
