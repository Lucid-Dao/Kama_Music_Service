package com.kanavi.automotive.kama.kama_music_service.common.util

import android.content.ContentUris
import android.net.Uri
import androidx.core.net.toUri
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.artist.Artist
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song

object MusicUtil {
    fun getMediaStoreAlbumCoverUri(albumId: Long): Uri {
        val sArtworkUri = "content://media/external/audio/albumart".toUri()
        return ContentUris.withAppendedId(sArtworkUri, albumId)
    }


    fun splitIntoArtist(listSong: ArrayList<Song>): ArrayList<Artist> {
        val artists = arrayListOf<Artist>()
        val songGroupedByArtist = listSong.groupBy { it.artist }
        songGroupedByArtist.forEach { (artistName, listSongByArtist) ->
            val songCount = listSongByArtist.size
            if (songCount > 0) {
                val albumCount = listSongByArtist.groupBy { it.album }.size
                val artistId = listSongByArtist.first().artistId
                val artist = Artist(
                    id = artistId,
                    title = artistName,
                    songCount = songCount,
                    albumCount = albumCount,
                    albumArt = "",
                    usbId = listSongByArtist[0].usbId
                )
                artists.add(artist)
            }
        }
        return artists
    }

    fun splitIntoAlbums(listSong: ArrayList<Song>): ArrayList<Album> {
        val albums = arrayListOf<Album>()
        val songGroupedByAlbum = listSong.groupBy { it.album }
        songGroupedByAlbum.forEach { (albumName, songByAlbum) ->
            val albumId = songByAlbum.first().albumId
            val songCount = songByAlbum.size
            if (songCount > 0) {
                val song = songByAlbum.first()
                val artistName = song.artist
                val year = song.year
                val album = Album(
                    id = albumId,
                    artist = artistName,
                    title = albumName,
                    songCount = songCount,
                    coverArt = "",
                    year = year,
                    artistId = song.artistId,
                    dateAdded = song.dateAdded,
                    usbId = song.usbId
                )
                albums.add(album)
            }
        }
        return albums
    }

    fun indexOfSongInList(songs: List<Song>, songPath: String): Int {
        return songs.indexOfFirst { it.path == songPath }
    }
}