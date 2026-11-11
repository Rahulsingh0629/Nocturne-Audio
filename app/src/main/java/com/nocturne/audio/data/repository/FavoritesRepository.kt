package com.nocturne.audio.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface FavoritesRepository {
    fun getFavoriteSongIds(): Flow<Set<String>>
    fun getFavoriteSongs(): Flow<List<Song>>
    suspend fun toggleFavorite(song: Song): Result<Boolean>
    suspend fun isFavorite(songId: String): Boolean
}

class FavoritesRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FavoritesRepository {

    override fun getFavoriteSongIds(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }

        awaitClose { listener.remove() }
    }

    override fun getFavoriteSongs(): Flow<List<Song>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val sourceString = data["source"] as? String ?: "SAAVN"
                    val source = try { SongSource.valueOf(sourceString) } catch (_: Exception) { SongSource.SAAVN }

                    Song(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        artist = data["artist"] as? String ?: "",
                        albumArtUrl = data["albumArtUrl"] as? String ?: "",
                        audioUrl = data["audioUrl"] as? String ?: "",
                        durationSeconds = (data["durationSeconds"] as? Long)?.toInt() ?: 0,
                        year = (data["year"] as? Long)?.toInt() ?: 0,
                        language = data["language"] as? String ?: "",
                        source = source,
                        isFavorite = true
                    )
                } ?: emptyList()
                trySend(songs)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun toggleFavorite(song: Song): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        val favRef = firestore.collection("users")
            .document(uid)
            .collection("favorites")
            .document(song.id)

        return try {
            val doc = favRef.get().await()
            if (doc.exists()) {
                favRef.delete().await()
                Result.success(false) // Removed
            } else {
                val songMap = mapOf(
                    "title" to song.title,
                    "artist" to song.artist,
                    "albumArtUrl" to song.albumArtUrl,
                    "audioUrl" to song.audioUrl,
                    "durationSeconds" to song.durationSeconds,
                    "year" to song.year,
                    "language" to song.language,
                    "source" to song.source.name,
                    "addedAt" to System.currentTimeMillis()
                )
                favRef.set(songMap).await()
                Result.success(true) // Added
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(songId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(songId)
                .get()
                .await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}
