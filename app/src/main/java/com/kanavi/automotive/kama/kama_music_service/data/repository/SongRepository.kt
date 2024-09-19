package com.kanavi.automotive.kama.kama_music_service.data.repository

import com.kanavi.automotive.kama.kama_music_service.data.database.dao.SongDAO
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val songDAO: SongDAO) {

    fun getListSongFromArtist(artistId: Long) = songDAO.getListSongFromArtist(artistId)
    fun getListSongFromAlbum(album: String) = songDAO.getListSongFromAlbum(album)
    fun getListSongFromFolder(folderName: String) = songDAO.getListSongFromFolder(folderName)
    fun getSongWithMediaStoreId(mediaStoreId: Long) = songDAO.getSongWithMediaStoreId(mediaStoreId)

    suspend fun insert(song: Song) = songDAO.insert(song)
    suspend fun insertAll(songs: List<Song>) = songDAO.insertAll(songs)

    suspend fun getSongFromPath(path: String) = songDAO.getSongFromPath(path)

    suspend fun update(song: Song) = songDAO.update(song)

    suspend fun getAllSongFavorite(usbId: String) = songDAO.getAllFavorite(usbId)

    suspend fun getAll() = songDAO.getAll()

    suspend fun getAllByUsbID(usbId: String) = songDAO.getAllByUsbID(usbId)

    suspend fun updateSongInfo(newPath: String, artist: String, title: String, oldPath: String) =
        songDAO.updateSongInfo(newPath, artist, title, oldPath)

    suspend fun updateFolderName(folderName: String, id: Long) =
        songDAO.updateFolderName(folderName, id)

    suspend fun removeSong(mediaStoreId: Long) = songDAO.removeSong(mediaStoreId)
}