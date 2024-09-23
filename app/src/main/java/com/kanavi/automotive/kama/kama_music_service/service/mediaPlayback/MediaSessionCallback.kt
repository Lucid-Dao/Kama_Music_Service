package com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant
import com.kanavi.automotive.kama.kama_music_service.common.extension.getParentPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.isAudioFast
import com.kanavi.automotive.kama.kama_music_service.common.util.MusicUtil
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.service.MusicService
import com.kanavi.automotive.kama.kama_music_service.service.MusicService.Companion.CYCLE_REPEAT
import com.kanavi.automotive.kama.kama_music_service.service.MusicService.Companion.TOGGLE_SHUFFLE
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MediaIDHelper
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MusicProvider
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.TreeNode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class MediaSessionCallback(
    private val musicService: MusicService
) : MediaSessionCompat.Callback(), KoinComponent {
    private val musicProvider: MusicProvider by inject()

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPlayFromMediaId(mediaId, extras)
        val musicId = MediaIDHelper.extractMusicID(mediaId!!)
        Timber.d("onPlayFromMediaId: $mediaId  musicPath: $musicId")

        val playingQueue: ArrayList<Song> = ArrayList()
        when (MediaIDHelper.extractCategory(mediaId)) {

            MediaConstant.MEDIA_ID_MUSICS_BY_FILE -> {
                MediaIDHelper.extractMusicID(mediaId)?.getParentPath()?.let { folderPath ->
                    Timber.d("folder path: $folderPath")
                    musicProvider.getUsbSource()?.treeNode?.let {
                        TreeNode.findNode(it, folderPath)?.children?.forEach { node ->
                            node.value.isAudioFast().let {
                                val song =
                                    musicProvider.getUsbSource()?.songMap?.get(node.value.hashCode())
                                if (song != null) {
                                    Timber.d("add song to playlist: $song")
                                    playingQueue.add(song)
                                }
                            }
                        }
                        var songIndex = MusicUtil.indexOfSongInList(playingQueue, musicId!!)
                        if (songIndex == -1) {
                            songIndex = 0
                        }
                        musicService.openQueue(playingQueue, songIndex, true)
                    }
                }
            }

            MediaConstant.MEDIA_ID_MUSICS_BY_SONGS -> {
                val tracks = musicProvider.getUsbSource()?.songs
                tracks?.let {
                    musicId?.let {
                        playingQueue.addAll(tracks)
                        var songIndex = MusicUtil.indexOfSongInList(tracks, musicId)
                        if (songIndex == -1) {
                            songIndex = 0
                        }
                        musicService.openQueue(playingQueue, songIndex, true)
                    }
                }
            }

            MediaConstant.MEDIA_ID_MUSICS_BY_ALBUM -> {
                val currentSongPath = MediaIDHelper.extractMusicID(mediaId)
                val song = musicProvider.getUsbSource()?.songMap?.get(currentSongPath.hashCode())


                val tracks = musicProvider.getUsbSource()?.songInAlbumDB?.get(song?.id)
                val songsInSameAlbum = mutableListOf<Song>()
                tracks?.forEach {
                    songsInSameAlbum.add(it)
                }
                playingQueue.addAll(songsInSameAlbum)

                var songIndex = MusicUtil.indexOfSongInList(songsInSameAlbum, musicId!!)
                if (songIndex == -1) {
                    songIndex = 0
                }
                musicService.openQueue(playingQueue, songIndex, true)
            }

            MediaConstant.MEDIA_ID_MUSICS_BY_FAVORITE -> {
                val tracks = musicProvider.getUsbSource()?.songFavoriteInDB?.value

                tracks?.let {
                    musicId?.let {
                        playingQueue.addAll(tracks)
                        var songIndex = MusicUtil.indexOfSongInList(tracks, musicId)
                        if (songIndex == -1) {
                            songIndex = 0
                        }
                        musicService.openQueue(playingQueue, songIndex, true)
                    }
                }
            }
        }

    }

    override fun onPrepare() {
        super.onPrepare()
        Timber.d("onPrepare")
        musicService.restoreState(::onPlay)
    }

    override fun onPlay() {
        super.onPlay()
        Timber.d("onPlay")
        musicService.play()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        musicService.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        Timber.d("onSkipToNext")
        musicService.playNextSong(true)
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        Timber.d("onSkipToPrevious")
        musicService.playPreviousSong(true)
    }

    override fun onStop() {
        super.onStop()
        Timber.d("onStop")
        musicService.pause()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        Timber.d("onSeekTo pos: $pos")
        musicService.seek(pos.toInt())
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        when (action) {
            CYCLE_REPEAT -> {
                Timber.d("TOGGLE CYCLE_REPEAT")
                musicService.cycleRepeatMode()
                musicService.updateMediaSessionPlaybackState()
            }

            TOGGLE_SHUFFLE -> {
                Timber.d("TOGGLE SHUFFLE")
                musicService.toggleShuffle()
                musicService.updateMediaSessionPlaybackState()
            }

            else -> {
                Timber.d("Unsupported action: $action")
            }
        }
    }
}