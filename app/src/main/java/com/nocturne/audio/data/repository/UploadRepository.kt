package com.nocturne.audio.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import com.nocturne.audio.model.UploadItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface UploadRepository {
    fun getUserUploads(): Flow<List<Song>>
    suspend fun uploadAudioFile(
        context: Context,
        fileUri: Uri,
        title: String,
        artist: String,
        year: Int,
        language: String,
        onProgress: (Float) -> Unit
    ): Result<UploadItem>
    fun cancelCurrentUpload()
    suspend fun deleteUpload(uploadId: String, storagePath: String): Result<Unit>
}

class UploadRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : UploadRepository {

    private var currentUploadTask: UploadTask? = null

    override fun getUserUploads(): Flow<List<Song>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .collection("uploads")
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val uploads = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "Untitled"
                    val artist = doc.getString("artist") ?: "Unknown Artist"
                    val year = (doc.getLong("year") ?: 0L).toInt()
                    val language = doc.getString("language") ?: "English"
                    val storagePath = doc.getString("storagePath") ?: ""
                    val downloadUrl = doc.getString("downloadUrl") ?: ""
                    val durationSeconds = (doc.getLong("durationSeconds") ?: 0L).toInt()

                    if (downloadUrl.isBlank()) return@mapNotNull null

                    Song(
                        id = doc.id,
                        title = title,
                        artist = artist,
                        albumArtUrl = "",
                        audioUrl = downloadUrl,
                        durationSeconds = durationSeconds,
                        year = year,
                        language = language,
                        source = SongSource.UPLOAD,
                        isFavorite = false
                    )
                } ?: emptyList()
                trySend(uploads)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun uploadAudioFile(
        context: Context,
        fileUri: Uri,
        title: String,
        artist: String,
        year: Int,
        language: String,
        onProgress: (Float) -> Unit
    ): Result<UploadItem> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated."))
        val contentResolver = context.contentResolver

        // Validate MIME type
        val mimeType = contentResolver.getType(fileUri) ?: "audio/mpeg"
        if (!mimeType.startsWith("audio/")) {
            return Result.failure(Exception("Invalid file format. Only audio files are allowed."))
        }

        // Validate File Size (Max 100 MB = 104,857,600 bytes)
        var fileSize = 0L
        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (fileSize > 104857600) {
            return Result.failure(Exception("File exceeds maximum allowed size of 100 MB."))
        }

        val sanitizedTitle = title.trim().take(100)
        val sanitizedArtist = artist.trim().take(100)
        val safeYear = year.coerceIn(1900, 2100)

        val uniqueFileName = "${System.currentTimeMillis()}_${fileUri.lastPathSegment?.replace("[^a-zA-Z0-9._-]".toRegex(), "_") ?: "track.mp3"}"
        val storagePath = "users/$uid/audio/$uniqueFileName"
        val storageRef = storage.reference.child(storagePath)

        return try {
            val uploadTask = storageRef.putFile(fileUri)
            currentUploadTask = uploadTask

            uploadTask.addOnProgressListener { taskSnapshot ->
                if (taskSnapshot.totalByteCount > 0) {
                    val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount.toFloat()
                    onProgress(progress)
                }
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            val docRef = firestore.collection("users").document(uid).collection("uploads").document()
            val uploadItem = UploadItem(
                id = docRef.id,
                title = sanitizedTitle,
                artist = sanitizedArtist,
                year = safeYear,
                language = language,
                storagePath = storagePath,
                sizeBytes = fileSize,
                uploadedAt = System.currentTimeMillis()
            )

            val firestoreMap = mapOf(
                "title" to uploadItem.title,
                "artist" to uploadItem.artist,
                "year" to uploadItem.year,
                "language" to uploadItem.language,
                "storagePath" to uploadItem.storagePath,
                "downloadUrl" to downloadUrl,
                "sizeBytes" to uploadItem.sizeBytes,
                "uploadedAt" to uploadItem.uploadedAt
            )

            docRef.set(firestoreMap).await()
            currentUploadTask = null
            Result.success(uploadItem)
        } catch (e: Exception) {
            currentUploadTask = null
            Result.failure(e)
        }
    }

    override fun cancelCurrentUpload() {
        currentUploadTask?.cancel()
        currentUploadTask = null
    }

    override suspend fun deleteUpload(uploadId: String, storagePath: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in."))
        return try {
            if (storagePath.isNotBlank()) {
                storage.reference.child(storagePath).delete().await()
            }
            firestore.collection("users")
                .document(uid)
                .collection("uploads")
                .document(uploadId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
