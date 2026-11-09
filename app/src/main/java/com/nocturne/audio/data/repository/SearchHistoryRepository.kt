package com.nocturne.audio.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nocturne.audio.model.RecentSearch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface SearchHistoryRepository {
    fun getRecentSearches(): Flow<List<RecentSearch>>
    suspend fun addRecentSearch(query: String): Result<Unit>
    suspend fun removeRecentSearch(searchId: String): Result<Unit>
    suspend fun clearAllRecentSearches(): Result<Unit>
}

class SearchHistoryRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SearchHistoryRepository {

    override fun getRecentSearches(): Flow<List<RecentSearch>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("recentSearches")
            .orderBy("at", Query.Direction.DESCENDING)
            .limit(8) // Max 8 recent searches
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val searches = snapshot?.documents?.mapNotNull { doc ->
                    val query = doc.getString("query") ?: return@mapNotNull null
                    val at = doc.getLong("at") ?: 0L
                    RecentSearch(id = doc.id, query = query, at = at)
                } ?: emptyList()
                trySend(searches)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addRecentSearch(query: String): Result<Unit> {
        val trimmedQuery = query.trim().take(100) // Sanitize and length limit max 100 chars
        if (trimmedQuery.isBlank()) return Result.success(Unit)

        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val collection = firestore.collection("users").document(uid).collection("recentSearches")

        return try {
            // Check if query already exists to update timestamp instead of duplicating
            val existing = collection.whereEqualTo("query", trimmedQuery).get().await()
            if (!existing.isEmpty) {
                val docId = existing.documents.first().id
                collection.document(docId).update("at", System.currentTimeMillis()).await()
            } else {
                val newDoc = collection.document()
                val data = mapOf(
                    "query" to trimmedQuery,
                    "at" to System.currentTimeMillis()
                )
                newDoc.set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeRecentSearch(searchId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("recentSearches")
                .document(searchId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllRecentSearches(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val collection = firestore.collection("users").document(uid).collection("recentSearches")
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
