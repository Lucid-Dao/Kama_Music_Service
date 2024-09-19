package com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.service.MusicService
import java.lang.ref.WeakReference


class PlaybackManager(val context: Context) {
    private var mMusicService: WeakReference<MusicService>? = null
    fun setMusicService(service: MusicService) {
        mMusicService = WeakReference(service)
    }

    var playback: Playback? = null

    val audioSessionId: Int
        get() = if (playback != null) {
            playback!!.audioSessionId
        } else 0

    val songDurationMillis: Int
        get() = if (playback != null) {
            playback!!.duration()
        } else -1

    val songProgressMillis: Int
        get() = if (playback != null) {
            playback!!.position()
        } else -1

    val isPlaying: Boolean
        get() = playback != null && playback!!.isPlaying

    init {
        playback = createLocalPlayback()
    }

    fun setCallbacks(callbacks: Playback.PlaybackCallbacks) {
        playback?.callbacks = callbacks
    }

    fun play(onNotInitialized: () -> Unit) {
        if (playback != null && !playback!!.isPlaying) {
            if (!playback!!.isInitialized) {
                onNotInitialized()
            } else {
                openAudioEffectSession()
                if (playback is CrossFadePlayer) {
                    if (!(playback as CrossFadePlayer).isCrossFading) {
                        AudioFader.startFadeAnimator(playback!!, true)
                    }
                } else {
                    AudioFader.startFadeAnimator(playback!!, true)
                }
                playback?.start()
            }
        }
    }

    fun pause(force: Boolean, onPause: () -> Unit) {
        if (playback != null && playback!!.isPlaying) {
            if (force) {
                playback?.pause()
                closeAudioEffectSession()
                onPause()
            } else {
                AudioFader.startFadeAnimator(playback!!, false) {
                    //Code to run when Animator Ends
                    playback?.pause()
                    closeAudioEffectSession()
                    onPause()
                }
            }
        }
    }

    fun stop() {
        if (playback != null) {
            playback?.stop()
            closeAudioEffectSession()
        }
    }

    fun seek(millis: Int, force: Boolean): Int = playback!!.seek(millis, force)

    fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        playback?.setDataSource(song, force, completion)
    }

    fun setNextDataSource(songPath: String?) {
        playback?.setNextDataSource(songPath)
    }

    fun setCrossFadeDuration(duration: Int) {
        playback?.setCrossFadeDuration(duration)
    }

    /**
     * @param crossFadeDuration CrossFade duration
     * @return Whether switched playback
     */
    fun maybeSwitchToCrossFade(crossFadeDuration: Int): Boolean {
        /* Switch to MultiPlayer if CrossFade duration is 0 and
                Playback is not an instance of MultiPlayer */
        if (playback !is MultiPlayer && crossFadeDuration == 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = MultiPlayer(context)
            return true
        } else if (playback !is CrossFadePlayer && crossFadeDuration > 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = CrossFadePlayer(context)
            return true
        }
        return false
    }


    fun release() {
        playback?.release()
        playback = null
        closeAudioEffectSession()
    }

    private fun openAudioEffectSession() {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)
    }

    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        if (playback != null) {
            audioEffectsIntent.putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                playback!!.audioSessionId
            )
        }
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    private fun createLocalPlayback(): Playback {
        // Set MultiPlayer when crossfade duration is 0 i.e. off
        return if (AudioFader.CROSS_FADE_DURATION == 0) {
            MultiPlayer(context)
        } else {
            val crossFadePlayer = CrossFadePlayer(context)
            crossFadePlayer.setMusicService(mMusicService)
            crossFadePlayer
        }
    }

    fun setPlaybackSpeedPitch(playbackSpeed: Float, playbackPitch: Float) {
        playback?.setPlaybackSpeedPitch(playbackSpeed, playbackPitch)
    }
}