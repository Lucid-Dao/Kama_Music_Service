package com.kanavi.automotive.kama.kama_music_service.common.util

import android.content.Context
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant
import com.kanavi.automotive.kama.kama_music_service.data.database.model.FileDirItem
import com.kanavi.automotive.kama.kama_music_service.common.extension.containsNoMedia
import com.kanavi.automotive.kama.kama_music_service.common.extension.getAlbum
import com.kanavi.automotive.kama.kama_music_service.common.extension.getAlbumIdAndArtistIdFromPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getArtist
import com.kanavi.automotive.kama.kama_music_service.common.extension.getDuration
import com.kanavi.automotive.kama.kama_music_service.common.extension.getFilenameFromPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getMediaStoreIdFromPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getMediaStoreLastModified
import com.kanavi.automotive.kama.kama_music_service.common.extension.getParentPath
import com.kanavi.automotive.kama.kama_music_service.common.extension.getUsbID
import com.kanavi.automotive.kama.kama_music_service.common.extension.getYear
import com.kanavi.automotive.kama.kama_music_service.common.extension.isAudioFast
import com.kanavi.automotive.kama.kama_music_service.data.database.model.ListItem
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MediaIDHelper
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MusicProvider
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.TreeNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File

class IntNumber(var value: Int) {
    fun increase() {
        value++
    }
}

object UsbUtil : KoinComponent {
    private val musicProvider: MusicProvider by inject()
    private val context: Context by inject()

    private const val STEP_SONG_TO_UPDATE = 100

    fun scanAllMusicFromUsb(
        rootPath: String,
        pathsToIgnore: List<String>,
        songList: ArrayList<Song>,
        treeNode: TreeNode,
        songInDb: MutableStateFlow<List<Song>>,
        albumInDb: MutableStateFlow<List<Album>>,
        favoriteInDb: MutableStateFlow<List<Song>>,
        songInAlbumInDb: HashMap<Long, List<Song>>

    ) {
        Timber.e("===========START SCAN ALL MEDIA FILES FROM USB ID: $rootPath============")

        Timber.d("rootPath is $rootPath")
        if (rootPath.isEmpty()) {
            return
        }

        val rootFile = File(rootPath)
        val numberFileCount = IntNumber(0)
        findAudioFiles(
            rootFile,
            pathsToIgnore,
            numberFileCount,
            songList,
            treeNode
        )
        val itemUsbMediaID = MediaIDHelper.createMediaID(
            musicProvider.getSelectedUsbID(),
            MediaConstant.MEDIA_ID_ROOT_USB
        )
        musicProvider.notifyDataChanged(
            listOf(itemUsbMediaID, musicProvider.currentParentID)
        )
        if (songList.isEmpty()) {
            Timber.e("Have no song in usb: $rootPath")
        }
        DBHelper.updateAllDatabase(songList)
        CoroutineScope(Dispatchers.IO).launch {
            val usbId = rootPath.getUsbID()

            songInDb.value = DBHelper.getAllSongFromUsb(usbId)
            albumInDb.value = DBHelper.getAllAlbumFromUsb(usbId)
            val favoriteList = DBHelper.getAllSongFavorite(usbId)
            if (favoriteList != null) {
                favoriteInDb.value = favoriteList
            }

            albumInDb.value.forEach {
                songInAlbumInDb[it.id] = DBHelper.getAllSongFromAlbum(it.title)
            }
        }
        Timber.e("=============FINISH SCAN ALL MEDIA FILES FROM USB ID: $rootPath - size of list is ${songList.size}============")
    }

    private fun findAudioFiles(
        file: File,
        excludedPaths: List<String>,
        numberFileCount: IntNumber,
        songList: ArrayList<Song>,
        treeNode: TreeNode,
    ) {

        Timber.i("scanning file with absolutePath: ${file.absolutePath} path: ${file.path}")
        if (file.isHidden) {
            Timber.i("file is hidden")
            return
        }

        val path = file.absolutePath
        if (path in excludedPaths || path.getParentPath() in excludedPaths) {
            Timber.i("path in excludedPaths: $path")
            return
        }
        Timber.d("Path is $path")

        if (file.isFile) {
            Timber.d("File is file: $file")
            if (path.isAudioFast()) {
                Timber.i("$path is a audio ")
                val song = getSongFromPath(path)
                song.let {
                    songList.add(it)
                }
                numberFileCount.increase()
                TreeNode.createTree(listOf(path), treeNode)
                if (numberFileCount.value % STEP_SONG_TO_UPDATE == STEP_SONG_TO_UPDATE - 1) {
                    val itemUsbMediaID = MediaIDHelper.createMediaID(
                        musicProvider.getSelectedUsbID(),
                        MediaConstant.MEDIA_ID_ROOT_USB
                    )
                    musicProvider.notifyDataChanged(
                        listOf(itemUsbMediaID, musicProvider.currentParentID)
                    )
                }
            } else {
                Timber.i("$path is NOT audio")
            }
        } else if (!file.containsNoMedia()) {
            file.listFiles().orEmpty().forEach { child ->
                findAudioFiles(
                    child,
                    excludedPaths,
                    numberFileCount,
                    songList,
                    treeNode
                )
            }
        }
    }

    private fun getSongFromPath(path: String): Song {
        val mediaStoreID = context.getMediaStoreIdFromPath(path)
        val title = path.getFilenameFromPath()

        val artist = context.getArtist(path)
        val duration = context.getDuration(path)
        val folderName = path.getParentPath().getFilenameFromPath()
        val album = context.getAlbum(path)
        val year = context.getYear(path)
        val lastModified = context.getMediaStoreLastModified(path)
        val usbID = path.getUsbID()
        val pair = context.getAlbumIdAndArtistIdFromPath(path)
        val albumId = pair.first
        val artistId = pair.second

        val song = Song(
            id = 0,
            mediaStoreId = mediaStoreID,
            title = title,
            artist = artist ?: "",
            path = path,
            duration = duration ?: 0,
            album = album ?: "",
            coverArt = "",
            folderName = folderName,
            albumId = albumId,
            artistId = artistId,
            year = year,
            dateAdded = lastModified,
            usbId = usbID
        )
        Timber.i("song from path $path is $song")
        return song
    }

    fun scanAllMusicFromUsbNotMetaData(
        rootPath: String,
        pathsToIgnore: List<String>,
        songList: ArrayList<Song>,
        songMap: HashMap<Int, Song>,
        treeNode: TreeNode,
    ) {
        Timber.e("===========START SCAN ALL MEDIA FILES NOT METADATA FROM USB ID: $rootPath============")

        if (rootPath.isEmpty()) {
            Timber.i("rootPath is empty")
            return
        }

        val rootFile = File(rootPath)
        val numberFileCount = IntNumber(0)
        findAudioFilesNotMetaData(
            rootFile,
            pathsToIgnore,
            numberFileCount,
            songList,
            songMap,
            treeNode
        )
        musicProvider.notifyDataChanged()

        if (songList.isEmpty()) {
            Timber.e("Have no song in usb: $rootPath")
        }
        Timber.e("=============FINISH SCAN ALL MEDIA FILES NOT METADATA FROM USB ID: $rootPath - size of list is ${songList.size}============")
    }

    private fun findAudioFilesNotMetaData(
        file: File,
        excludedPaths: List<String>,
        numberFileCount: IntNumber,
        songList: ArrayList<Song>,
        songMap: HashMap<Int, Song>,
        treeNode: TreeNode,
    ) {

        Timber.i("scanning file with absolutePath: ${file.absolutePath} path: ${file.path}")
        if (file.isHidden) {
            Timber.i("file is hidden")
            return
        }

        val path = file.absolutePath
        if (path in excludedPaths || path.getParentPath() in excludedPaths) {
            Timber.i("path in excludedPaths: $path")
            return
        }

        if (file.isFile) {
            if (path.isAudioFast()) {
                Timber.i("$path is a audio ")
                val song = getSongNotMetaDataFromPath(path)
                songList.add(song)
                songMap[path.hashCode()] = song


                numberFileCount.increase()
                TreeNode.createTree(listOf(path), treeNode)
//                musicProvider.notifyDataChanged()
            } else {
                Timber.i("$path is NOT audio")
            }
        } else if (!file.containsNoMedia()) {
            file.listFiles().orEmpty().forEach { child ->
                findAudioFilesNotMetaData(
                    child,
                    excludedPaths,
                    numberFileCount,
                    songList,
                    songMap,
                    treeNode
                )
            }
        }
    }

    private fun getSongNotMetaDataFromPath(path: String): Song {
        val title = path.getFilenameFromPath()
        val song = Song(
            id = 0,
            title = title,
            path = path,
        )
        Timber.i("song from path $path is $song")
        return song
    }

    fun getListItemsFromFileDirItems(fileDirItems: ArrayList<FileDirItem>): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>()
        fileDirItems.forEach {
            val listItem =
                ListItem(it.path, it.name, false, 0, it.size, it.modified, false, false)
            listItems.add(listItem)
        }
        return listItems
    }
}