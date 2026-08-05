package com.kaganim.fairyai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.kaganim.fairyai.domain.model.User
import com.kaganim.fairyai.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            trySend(firebaseUser?.let { User(it.uid, it.email ?: "", it.displayName) })
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user?.let { User(it.uid, it.email ?: "", it.displayName) }
            if (user != null) Result.success(user) else Result.failure(Exception("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user?.let { User(it.uid, it.email ?: "", it.displayName) }
            if (user != null) Result.success(user) else Result.failure(Exception("Registration failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
