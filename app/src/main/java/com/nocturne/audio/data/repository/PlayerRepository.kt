package com.nocturne.audio.data.repository

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nocturne.audio.model.Song
import com.nocturne.audio.model.SongSource
import com.nocturne.audio.player.NocturnePlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

interface PlayerRepository {
    val currentSong: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val progressFraction: StateFlow<Float>
    val repeatMode: StateFlow<RepeatMode>
    val isShuffleEnabled: StateFlow<Boolean>
    val queue: StateFlow<List<Song>>
    val qualityLabel: StateFlow<String>
    val playbackError: StateFlow<String?>

    fun initialize(context: Context)
    fun playSong(song: Song, newQueue: List<Song> = listOf(song))
    fun playQueue(newQueue: List<Song>, startIndex: Int = 0)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun toggleRepeatMode()
    fun toggleShuffle()
    fun clearError()
}

class PlayerRepositoryImpl private constructor() : PlayerRepository {

    companion object {
        @Volatile
        private var INSTANCE: PlayerRepositoryImpl? = null

        fun getInstance(): PlayerRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerRepositoryImpl().also { INSTANCE = it }
            }
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    override val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    override val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    override val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _qualityLabel = MutableStateFlow("")
    override val qualityLabel: StateFlow<String> = _qualityLabel.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    override val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    override fun initialize(context: Context) {
        if (mediaController != null || controllerFuture != null) return

        val sessionToken = SessionToken(context, ComponentName(context, NocturnePlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                mediaController = controller
                setupPlayerListener(controller)
            } catch (e: Exception) {
                _playbackError.value = "Failed to initialize media player controller."
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        _durationMs.value = controller.duration.coerceAtLeast(0L)
                        updateProgress()
                    }
                    Player.STATE_ENDED -> {
                        handleSongCompletion()
                    }
                    else -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId
                if (songId != null) {
                    val matchingSong = _queue.value.find { it.id == songId }
                    if (matchingSong != null) {
                        _currentSong.value = matchingSong
                        updateQualityLabel(matchingSong)
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playbackError.value = "Unable to play track. Skipping to next..."
                skipToNext()
            }
        })
    }

    override fun playSong(song: Song, newQueue: List<Song>) {
        _queue.value = if (newQueue.contains(song)) newQueue else listOf(song) + newQueue
        _currentSong.value = song
        updateQualityLabel(song)
        playSongInternal(song)
    }

    override fun playQueue(newQueue: List<Song>, startIndex: Int) {
        if (newQueue.isEmpty()) return
        _queue.value = newQueue
        val safeIndex = startIndex.coerceIn(0, newQueue.lastIndex)
        val targetSong = newQueue[safeIndex]
        _currentSong.value = targetSong
        updateQualityLabel(targetSong)
        playSongInternal(targetSong)
    }

    private fun playSongInternal(song: Song) {
        val controller = mediaController ?: return

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(if (song.albumArtUrl.isNotBlank()) android.net.Uri.parse(song.albumArtUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.audioUrl)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    override fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0)
            }
            controller.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        if (_durationMs.value > 0) {
            _progressFraction.value = (positionMs.toFloat() / _durationMs.value.toFloat()).coerceIn(0f, 1f)
        }
    }

    override fun skipToNext() {
        val current = _currentSong.value ?: return
        val queueList = _queue.value
        if (queueList.isEmpty()) return

        val currentIndex = queueList.indexOfFirst { it.id == current.id }
        val nextIndex = if (_isShuffleEnabled.value) {
            (queueList.indices).random()
        } else {
            (currentIndex + 1) % queueList.size
        }

        val nextSong = queueList[nextIndex]
        playSong(nextSong, queueList)
    }

    override fun skipToPrevious() {
        val current = _currentSong.value ?: return
        val queueList = _queue.value
        if (queueList.isEmpty()) return

        val currentIndex = queueList.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else queueList.lastIndex

        val prevSong = queueList[prevIndex]
        playSong(prevSong, queueList)
    }

    override fun toggleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
    }

    override fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    override fun clearError() {
        _playbackError.value = null
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentSong.value?.let { playSongInternal(it) }
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                skipToNext()
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (_isPlaying.value) {
                updateProgress()
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
    }

    private fun updateProgress() {
        val controller = mediaController ?: return
        val pos = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.coerceAtLeast(0L)

        _currentPositionMs.value = pos
        _durationMs.value = dur
        if (dur > 0) {
            _progressFraction.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
        }
    }

    private fun updateQualityLabel(song: Song) {
        _qualityLabel.value = when (song.source) {
            SongSource.JAMENDO -> "MP3 • 320kbps streaming"
            SongSource.LOCAL -> {
                val urlLower = song.audioUrl.lowercase()
                when {
                    urlLower.contains(".flac") -> "FLAC • local file"
                    urlLower.contains(".wav") -> "WAV • local file"
                    urlLower.contains(".m4a") || urlLower.contains(".aac") -> "AAC • local file"
                    else -> "MP3 • local file"
                }
            }
            SongSource.UPLOAD -> "AAC • private cloud"
        }
    }
}
