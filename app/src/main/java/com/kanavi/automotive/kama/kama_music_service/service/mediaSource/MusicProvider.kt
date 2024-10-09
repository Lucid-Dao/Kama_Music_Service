package com.kanavi.automotive.kama.kama_music_service.service.mediaSource

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import androidx.core.net.toUri
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_ALBUM
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_ARTIST
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_FAVORITE
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_FILE
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_SONGS
import com.kanavi.automotive.kama.kama_music_service.common.extension.isAudioFast
import com.kanavi.automotive.kama.kama_music_service.common.util.UsbUtil
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.service.MusicService
import timber.log.Timber
import java.lang.ref.WeakReference

class MusicProvider(private val mContext: Context) {
    private var mMusicService: WeakReference<MusicService>? = null

    fun setMusicService(service: MusicService) {
        mMusicService = WeakReference(service)
    }

    var currentParentID: String = ""

    private var selectedUsbID = ""
    fun getSelectedUsbID(): String = selectedUsbID


    fun setSelectedUsbID(usbID: String) {
        Timber.d("setSelectedUsbID: $usbID")
        selectedUsbID = usbID
    }

    private val usbSourceMap: HashMap<String, UsbSource> = HashMap()

    fun getUsbSource(usbID: String? = null): UsbSource? {
        return usbSourceMap[usbID ?: selectedUsbID]
    }

    fun addUsbSource(usbId: String, usbSource: UsbSource) {
        Timber.d("addUsbSource: $usbId")
        usbSourceMap[usbId] = usbSource
        if (selectedUsbID.isEmpty()) setSelectedUsbID(usbId)
        notifyDataChanged(
            listOf(
                MEDIA_ID_MUSICS_BY_SONGS,
                MEDIA_ID_MUSICS_BY_FILE,
                MEDIA_ID_MUSICS_BY_FAVORITE,
                MEDIA_ID_MUSICS_BY_ALBUM,
                currentParentID
            )
        )
    }

    fun removeUsbSource(usbId: String) {
        if (usbSourceMap[usbId] != null) {
            Timber.e("Map has UsbSource")
        }
        Timber.e("before removeUsbSource: $usbId => ${usbSourceMap.keys}")
        usbSourceMap.remove(usbId)
        Timber.e("after removeUsbSource: $usbId => ${usbSourceMap.keys}")
        val newSelectedUsbId = usbSourceMap.firstNotNullOfOrNull { it.key } ?: ""
        Timber.d("removeUsbSource: $usbId, current selectedUsbID: $newSelectedUsbId")
        setSelectedUsbID(newSelectedUsbId)
        notifyDataChanged(
            listOf(
                MEDIA_ID_MUSICS_BY_SONGS,
                MEDIA_ID_MUSICS_BY_FILE,
                MEDIA_ID_MUSICS_BY_FAVORITE,
                MEDIA_ID_MUSICS_BY_ALBUM,
                currentParentID
            )
        )
    }

    fun notifyDataChanged(mediaIdChanged: List<String>? = null) {
        Timber.e("==========notifyDataChanged is called=============")
        mediaIdChanged?.distinct()?.forEach { mediaId ->
            mMusicService?.get()?.notifyChildrenChanged(mediaId)
        }
    }

    fun getChildren(
        mediaId: String,
        usbID: String? = null
    ): List<MediaBrowserCompat.MediaItem> {
        Timber.d("for mediaId: $mediaId, current selectedUsbID: $selectedUsbID")
        currentParentID = mediaId
        val mediaItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
        var rootPath = MediaIDHelper.extractMusicID(mediaId)
        if (rootPath == null)
            rootPath = usbID ?: selectedUsbID

        Timber.d("rootPath is :$rootPath")

        when (mediaId) {
            MEDIA_ID_MUSICS_BY_SONGS -> {
                Timber.d("MEDIA_ID_MUSICS_BY_SONGS")
                mediaItems.addAll(getSongChildren(rootPath))
            }

            MEDIA_ID_MUSICS_BY_FILE -> {
                Timber.d("MEDIA_ID_MUSICS_BY_FILE")
                getFileChildren(mediaId, mediaItems, rootPath)
            }

            MEDIA_ID_MUSICS_BY_ALBUM -> {
                Timber.d("MEDIA_ID_MUSICS_BY_ALBUM")
                mediaItems.addAll(getAlbumChildren(mediaId, rootPath))
            }

            MEDIA_ID_MUSICS_BY_ARTIST -> {
                Timber.d("MEDIA_ID_MUSICS_BY_ARTIST")
                mediaItems.addAll(getArtistChildren(mediaId, selectedUsbID))
            }

            MEDIA_ID_MUSICS_BY_FAVORITE -> {
                Timber.d("MEDIA_ID_MUSICS_BY_FAVORITE")
                mediaItems.addAll(getSongChildrenFavorite(rootPath))

            }
            else -> {
                when {
                    mediaId.contains(MEDIA_ID_MUSICS_BY_FILE) ->
                        getFileChildren(mediaId, mediaItems, rootPath)

                    mediaId.contains(MEDIA_ID_MUSICS_BY_ALBUM) ->
                        mediaItems.addAll(getAlbumSongChildren(mediaId, rootPath))

                    mediaId.contains(MEDIA_ID_MUSICS_BY_ARTIST) ->
                        mediaItems.addAll(getArtistSongChildren(mediaId, rootPath))
                }
            }
        }

        Timber.d("mediaItems size: ${mediaItems.size}")

        return mediaItems
    }

    private fun getSongChildren(usbID: String? = null): List<MediaBrowserCompat.MediaItem> {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        val resources: List<Song>? = getUsbSource()?.songInDB?.value

        resources?.forEach {
            mediaItems.add(
                getPlayableSong(it, MEDIA_ID_MUSICS_BY_SONGS)
            )
        }
        return mediaItems
    }

    private fun getSongChildrenFavorite(usbID: String? = null): List<MediaBrowserCompat.MediaItem> {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val resources: List<Song>? = getUsbSource()?.songFavoriteInDB?.value
        Timber.d("resources favorite: ${resources?.size}")
        resources?.forEach {
            mediaItems.add(
                getPlayableSong(it, MEDIA_ID_MUSICS_BY_FAVORITE)
            )
        }
        return mediaItems
    }

    private fun getFileChildren(
        mediaID: String,
        mediaItems: MutableList<MediaBrowserCompat.MediaItem>,
        usbID: String? = null
    ) {
        Timber.d("for mediaID: $mediaID")
        currentParentID = mediaID
        val category = MediaIDHelper.extractCategory(mediaID)
        var rootPath = MediaIDHelper.extractMusicID(mediaID)
        if (rootPath == null)
            rootPath = usbID ?: selectedUsbID
        Timber.d("getFileChildren of usbID: $rootPath for category: $category")

        // get Root
        val rootNode = getUsbSource()?.let { TreeNode.findNode(it.treeNode, rootPath) }
        val children = rootNode?.children?.let { ArrayList(it) }
        children?.forEach {
            val listSong = mutableListOf<Song?>()
            val listFolderPath = mutableListOf<String>()
            val path = it.value
            Timber.i("checking path: $path")
            if (path.isAudioFast()) {
                val song = getUsbSource()?.songMap?.get(path.hashCode())
                listSong.add(song)
            } else {
                listFolderPath.add(path)
            }

            listSong.forEach { song ->
                song?.let {
                    mediaItems.add(getPlayableSong(song = song))
                }
            }

            listFolderPath.forEach { folderPath ->
                val folderName = folderPath.substringAfterLast("/")
                val folderMediaID = MediaIDHelper.createMediaID(
                    folderPath,
                    MEDIA_ID_MUSICS_BY_FILE
                )
                Timber.d("Selected usb: ${selectedUsbID.isNotEmpty()}")
                mediaItems.add(
                    0,
                    UsbMediaItem.with(mContext)
                        .mediaID(folderMediaID)
                        .asBrowsable()
                        .title(folderName)
                        .subTitle("")
                        .setExtraProperties(
                            isUsbAttached = selectedUsbID.isNotEmpty(),
                            itemType = UsbMediaItem.Builder.ITEM_FOLDER,
                            currentUsbSelected = selectedUsbID
                        )
                        .build()
                )
            }
        }
    }

    private fun getAlbumChildren(
        mediaId: String,
        usbID: String? = null
    ): List<MediaBrowserCompat.MediaItem> {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val albums = getUsbSource()?.albumInDB?.value?.sortedBy { it.title }
        if (albums != null) {
            for (album in albums) {
                val albumPath = MediaIDHelper.createMediaID(
                    album.id.toString(),
                    mediaId
                )
                var albumIcons = UsbUtil.getAlbumCoverUri(album.title)
                if (albumIcons == null) {
                    albumIcons = getUsbSource()?.songInAlbumDB?.get(album.id)?.firstOrNull()?.getUri()
                }
                mediaItems.add(
                    UsbMediaItem.with(mContext)
                        .mediaID(albumPath)
                        .title(album.title)
                        .subTitle(album.artist)
                        .icon(albumIcons)
                        .asBrowsable()
                        .setExtraProperties(
                            isUsbAttached = selectedUsbID.isNotEmpty(),
                            itemType = UsbMediaItem.Builder.ITEM_ALBUM,
                            path = album.coverArt,
                            songCount = album.songCount
                        )
                        .build()
                )
            }
        }
        return mediaItems
    }

    private fun getArtistChildren(
        mediaId: String,
        usbID: String? = null
    ): List<MediaBrowserCompat.MediaItem> {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val artists = getUsbSource(usbID)?.artistInDB?.value?.sortedBy { it.title }
        if (artists != null) {
            for (artist in artists) {
                val artistPath = MediaIDHelper.createMediaID(
                    artist.id.toString(),
                    mediaId
                )
//                val artistIcons = getUsbSource()?.songInArtistDB?.get(artist.id)?.firstOrNull()?.getUri()
                mediaItems.add(
                    UsbMediaItem.with(mContext)
                        .mediaID(artistPath)
                        .title(artist.title)
                        .asBrowsable()
                        .icon(Uri.parse(artist.albumArt))
                        .setExtraProperties(
                            isUsbAttached = selectedUsbID.isNotEmpty(),
                            itemType = UsbMediaItem.Builder.ITEM_ARTIST,
                            songCount = artist.songCount
                        )
                        .build()
                )
            }
        }
        return mediaItems
    }


    private fun getAlbumSongChildren(
        mediaId: String,
        usbID: String? = null
    ): List<MediaBrowserCompat.MediaItem> {

        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
//        val albumId = MediaIDHelper.extractMusicID(mediaId)
        val songList = getUsbSource()?.songInAlbumDB?.get(usbID?.toLong())
        songList?.forEach {
            mediaItems.add(getPlayableSong(it, MEDIA_ID_MUSICS_BY_ALBUM))
        }
        return mediaItems
    }

    private fun getArtistSongChildren(
        mediaId: String,
        usbID: String? = null
    ): List<MediaBrowserCompat.MediaItem> {

        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val songList = getUsbSource()?.songInArtistDB?.get(usbID?.toLong())
        songList?.forEach {
            mediaItems.add(getPlayableSong(it, MEDIA_ID_MUSICS_BY_ARTIST))
        }
        return mediaItems
    }

    private fun getPlayableSong(
        song: Song,
        category: String = MEDIA_ID_MUSICS_BY_FILE
    ): MediaBrowserCompat.MediaItem {
        val songPath = MediaIDHelper.createMediaID(
            song.path,
            category
        )
        return UsbMediaItem.with(mContext)
            .asPlayable()
            .mediaID(songPath)
            .title(song.title)
            .description(song.album)
            .subTitle(song.artist)
            .icon(song.coverArt.toUri())
            .setExtraProperties(
                isUsbAttached = selectedUsbID.isNotEmpty(),
                itemType = UsbMediaItem.Builder.ITEM_SONG,
                currentUsbSelected = selectedUsbID,
                duration = song.duration,
                path = song.path,
                isFavorite = song.favorite
            )
            .build()
    }

}
