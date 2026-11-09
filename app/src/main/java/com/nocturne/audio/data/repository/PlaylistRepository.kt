package com.nocturne.audio.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nocturne.audio.model.Playlist
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun createPlaylist(name: String): Result<Playlist>
    suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit>
}

class PlaylistRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("playlists")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val songIds = (doc.get("songIds") as? List<String>) ?: emptyList()
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    Playlist(id = doc.id, name = name, songIds = songIds, createdAt = createdAt)
                } ?: emptyList()
                trySend(playlists)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createPlaylist(name: String): Result<Playlist> {
        val trimmedName = name.trim().take(100)
        if (trimmedName.isBlank()) return Result.failure(Exception("Playlist name cannot be empty."))

        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        val docRef = firestore.collection("users").document(uid).collection("playlists").document()

        val playlist = Playlist(
            id = docRef.id,
            name = trimmedName,
            songIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )

        return try {
            val map = mapOf(
                "name" to playlist.name,
                "songIds" to playlist.songIds,
                "createdAt" to playlist.createdAt
            )
            docRef.set(map).await()
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit> {
        val trimmedName = newName.trim().take(100)
        if (trimmedName.isBlank()) return Result.failure(Exception("Playlist name cannot be empty."))

        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("playlists")
                .document(playlistId)
                .update("name", trimmedName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("playlists")
                .document(playlistId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("playlists")
                .document(playlistId)
                .update("songIds", FieldValue.arrayUnion(songId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        return try {
            firestore.collection("users")
                .document(uid)
                .collection("playlists")
                .document(playlistId)
                .update("songIds", FieldValue.arrayRemove(songId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
