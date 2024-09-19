package com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.PowerManager
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.service.MusicService
import com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback.AudioFader.CROSS_FADE_DURATION
import com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback.AudioFader.createFadeAnimator
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.ref.WeakReference

class CrossFadePlayer(context: Context) : LocalPlayback(context) {
    private var currentPlayer: CurrentPlayer = CurrentPlayer.NOT_SET
    private var player1 = MediaPlayer()
    private var player2 = MediaPlayer()
    private var durationListener = DurationListener()
    private var mIsInitialized = false
    private var hasDataSource: Boolean = false /* Whether first player has DataSource */
    private var nextDataSource: String? = null
    private var crossFadeAnimator: Animator? = null
    override var callbacks: Playback.PlaybackCallbacks? = null
    private var crossFadeDuration = CROSS_FADE_DURATION
    var isCrossFading = false

    private var mMusicService: WeakReference<MusicService>? = null
    fun setMusicService(service: WeakReference<MusicService>?) {
        mMusicService = service
    }

    init {
        player1.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player2.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        currentPlayer = CurrentPlayer.PLAYER_ONE
        Timber.i("init CrossFadePlayer to use")
    }

    override fun start(): Boolean {
        super.start()
        durationListener.start()
        resumeFade()
        return try {
            getCurrentPlayer()?.start()
            if (isCrossFading) {
                getNextPlayer()?.start()
            }
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override fun release() {
        stop()
        cancelFade()
        getCurrentPlayer()?.release()
        getNextPlayer()?.release()
        durationListener.cancel()
    }

    override fun stop() {
        super.stop()
        getCurrentPlayer()?.reset()
        mIsInitialized = false
    }

    override fun pause(): Boolean {
        super.pause()
        durationListener.stop()
        pauseFade()
        getCurrentPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        getNextPlayer()?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        return true
    }

    override fun seek(whereto: Int, force: Boolean): Int {
        if (force) {
            endFade()
        }
        getNextPlayer()?.stop()
        return try {
            getCurrentPlayer()?.seekTo(whereto)
            whereto
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    override fun setVolume(vol: Float): Boolean {
        cancelFade()
        return try {
            getCurrentPlayer()?.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override val isInitialized: Boolean
        get() = mIsInitialized

    override val isPlaying: Boolean
        get() = mIsInitialized && getCurrentPlayer()?.isPlaying == true

    override fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        Timber.d("Song path: ${song.path}")
        if (force) hasDataSource = false
        mIsInitialized = false
        /* We've already set DataSource if initialized is true in setNextDataSource */
        if (!hasDataSource) {
            getCurrentPlayer()?.let {
                setDataSourceImpl(it, song.path) { success ->
                    mIsInitialized = success
                    completion(success)
                }
            }
            hasDataSource = true
        } else {
            completion(true)
            mIsInitialized = true
        }
    }

    override fun setNextDataSource(path: String?) {
        // Store the next song path in nextDataSource, we'll need this just in case
        // if the user closes the app, then we can't get the nextSong from musicService
        // As MusicPlayerRemote won't have access to the musicService
        nextDataSource = path
    }

    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            getCurrentPlayer()?.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override val audioSessionId: Int
        get() = getCurrentPlayer()?.audioSessionId!!

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.duration!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * Gets the current position in audio.
     * @return The position in milliseconds
     */
    override fun position(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            getCurrentPlayer()?.currentPosition!!
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            -1
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mp == getCurrentPlayer()) {
            callbacks?.onTrackEnded()
        }
    }

    private fun getCurrentPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player1
            }

            CurrentPlayer.PLAYER_TWO -> {
                player2
            }

            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun getNextPlayer(): MediaPlayer? {
        return when (currentPlayer) {
            CurrentPlayer.PLAYER_ONE -> {
                player2
            }

            CurrentPlayer.PLAYER_TWO -> {
                player1
            }

            CurrentPlayer.NOT_SET -> {
                null
            }
        }
    }

    private fun crossFade(fadeInMp: MediaPlayer, fadeOutMp: MediaPlayer) {
        isCrossFading = true
        crossFadeAnimator = createFadeAnimator(context, fadeInMp, fadeOutMp) {
            crossFadeAnimator = null
            durationListener.start()
            isCrossFading = false
        }
        crossFadeAnimator?.start()
    }

    private fun endFade() {
        crossFadeAnimator?.end()
        crossFadeAnimator = null
    }

    private fun cancelFade() {
        crossFadeAnimator?.cancel()
        crossFadeAnimator = null
    }

    private fun pauseFade() {
        crossFadeAnimator?.pause()
    }

    private fun resumeFade() {
        if (crossFadeAnimator?.isPaused == true) {
            crossFadeAnimator?.resume()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Timber.e("Couldn't play this song, Error: what: $what, extra: $extra")
        mIsInitialized = false
        mp?.release()
        player1 = MediaPlayer()
        player2 = MediaPlayer()
        mIsInitialized = true
        mp?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        return false
    }

    enum class CurrentPlayer {
        PLAYER_ONE,
        PLAYER_TWO,
        NOT_SET
    }

    inner class DurationListener : CoroutineScope by crossFadeScope() {

        private var job: Job? = null

        fun start() {
            job?.cancel()
            job = launch {
                while (isActive) {
                    delay(250)
                    onDurationUpdated(position(), duration())
                }
            }
        }

        fun stop() {
            job?.cancel()
        }
    }

    fun onDurationUpdated(progress: Int, total: Int) {
        if (total > 0 && (total - progress).div(1000) == crossFadeDuration) {
            getNextPlayer()?.let { player ->
                val nextSong = mMusicService?.get()?.nextSong
                // Switch to other player (Crossfade) only if next song exists
                // If we get an empty song it's can be because the app was cleared from background
                if (nextSong != null) {
                    nextDataSource = null
                    setDataSourceImpl(player, nextSong.path) { success ->
                        if (success) switchPlayer()
                    }

                }
                // So we have to use the previously stored nextDataSource value
                else if (!nextDataSource.isNullOrEmpty()) {
                    setDataSourceImpl(player, nextDataSource!!) { success ->
                        if (success) switchPlayer()
                        nextDataSource = null
                    }
                }
            }
        }
    }

    private fun switchPlayer() {
        getNextPlayer()?.start()
        crossFade(getNextPlayer()!!, getCurrentPlayer()!!)
        currentPlayer =
            if (currentPlayer == CurrentPlayer.PLAYER_ONE || currentPlayer == CurrentPlayer.NOT_SET) {
                CurrentPlayer.PLAYER_TWO
            } else {
                CurrentPlayer.PLAYER_ONE
            }
        callbacks?.onTrackEndedWithCrossfade()
    }

    override fun setCrossFadeDuration(duration: Int) {
        crossFadeDuration = duration
    }

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        getCurrentPlayer()?.setPlaybackSpeedPitch(speed, pitch)
        if (getNextPlayer()?.isPlaying == true) {
            getNextPlayer()?.setPlaybackSpeedPitch(speed, pitch)
        }
    }
}

internal fun crossFadeScope(): CoroutineScope = CoroutineScope(Job() + Dispatchers.Default)

@SuppressLint("NewApi")
fun MediaPlayer.setPlaybackSpeedPitch(speed: Float, pitch: Float) {
    val wasPlaying = isPlaying
    playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch)
    if (!wasPlaying) {
        pause()
    }
}