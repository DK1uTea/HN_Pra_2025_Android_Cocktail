package com.example.cocktaildb.data.service

import com.example.cocktaildb.data.model.Checkmark
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CheckmarkFirebaseService {
    private val firestore = FirebaseFirestore.getInstance()
    private val checkmarksCollection = firestore.collection("checkmarks")

    suspend fun addCheckmark(uid: String, cocktailId: String): Result<String> {
        return try {
            val checkmarkId = UUID.randomUUID().toString()
            val checkmark = Checkmark(
                id = checkmarkId,
                uid = uid,
                cocktailId = cocktailId
            )
            checkmarksCollection.document(checkmarkId).set(checkmark).await()
            Result.success(checkmarkId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCheckmark(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                document.reference.delete().await()
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isCheckmarked(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .whereEqualTo("cocktailId", cocktailId)
                .get()
                .await()
            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserCheckmarks(uid: String): Result<List<Checkmark>> {
        return try {
            val querySnapshot = checkmarksCollection
                .whereEqualTo("uid", uid)
                .get().await()
            val checkmarks = querySnapshot.toObjects(Checkmark::class.java)
            Result.success(checkmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCheckmark(uid: String, cocktailId: String): Result<Boolean> {
        return try {
            // First check if the cocktail is already checkmarked
            val isCheckmarkedResult = isCheckmarked(uid, cocktailId)

            if (isCheckmarkedResult.isSuccess) {
                val isAlreadyCheckmarked = isCheckmarkedResult.getOrNull() ?: false

                if (isAlreadyCheckmarked) {
                    // If already checkmarked, remove it
                    removeCheckmark(uid, cocktailId)
                    Result.success(false) // Return false to indicate it was removed
                } else {
                    // If not checkmarked, add it
                    addCheckmark(uid, cocktailId)
                    Result.success(true) // Return true to indicate it was added
                }
            } else {
                // Propagate the original error
                isCheckmarkedResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
