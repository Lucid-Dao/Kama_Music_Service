package com.kanavi.automotive.kama.kama_music_service.data.repository

import com.kanavi.automotive.kama.kama_music_service.data.database.dao.AlbumDAO
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album

class AlbumRepository(private val albumDAO: AlbumDAO) {

    fun getAlbumWithSongs() = albumDAO.getAlbumWithSongs()
    fun getAll() = albumDAO.getAll()
    fun getAllByUsbID(usbId: String) = albumDAO.getAllByUsbID(usbId)
    fun getAlbumWithId(id: Long) = albumDAO.getAlbumWithId(id)
    fun getArtistAlbums(artistId: Long) = albumDAO.getArtistAlbums(artistId)

    suspend fun insert(album: Album) = albumDAO.insert(album)
    suspend fun insertAll(albums: List<Album>) = albumDAO.insertAll(albums)

    suspend fun deleteAlbum(id: Long) = albumDAO.deleteAlbum(id)
}