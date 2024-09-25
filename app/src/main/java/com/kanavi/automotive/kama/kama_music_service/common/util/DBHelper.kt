package com.kanavi.automotive.kama.kama_music_service.common.util

import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.artist.Artist
import com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite.Favorite
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.data.repository.AlbumRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.ArtistRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.FavoriteRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

object DBHelper : KoinComponent {
    private val songRepository: SongRepository by inject()
    private val albumRepository: AlbumRepository by inject()
    private val artistRepository: ArtistRepository by inject()

    private val favoriteRepository: FavoriteRepository by inject()

    suspend fun updateAllDatabase(listSong: ArrayList<Song>) = withContext(Dispatchers.IO) {

        songRepository.insertAll(listSong)

        val newAlbums = MusicUtil.splitIntoAlbums(listSong)
        albumRepository.insertAll(newAlbums)

        val newArtists = MusicUtil.splitIntoArtist(listSong)
        artistRepository.insertAll(newArtists)

        cleanupDatabase(listSong)

    }


    suspend fun updateDataFavorite(path: String, isFavorite: Boolean): Pair<Boolean, List<Song>?> {
        return withContext(Dispatchers.IO) {
            val song = songRepository.getSongFromPath(path)
            song.favorite = isFavorite
            songRepository.update(song)

            if (isFavorite) {
                val favorite = Favorite(id = 0, pathSong = path, usbId = song.usbId)
                favoriteRepository.insert(favorite)
            } else {
                favoriteRepository.removeFavoriteSong(path)
            }
            Timber.d("favorite size: ${favoriteRepository.getAllFavoriteByUsbID(song.usbId).size}")

            return@withContext Pair(song.favorite, songRepository.getAllSongFavorite(song.usbId))
        }

    }

    suspend fun getAllSong() = withContext(Dispatchers.IO) { songRepository.getAll() }

    suspend fun getAllSongFavorite(usbId: String) =
        withContext(Dispatchers.IO) { songRepository.getAllSongFavorite(usbId) }

    suspend fun getAllSongFromUsb(usbId: String) =
        withContext(Dispatchers.IO) { songRepository.getAllByUsbID(usbId) }

    suspend fun getSongFromPath(path: String) =
        withContext(Dispatchers.IO) { songRepository.getSongFromPath(path) }

    suspend fun getAllAlbumFromUsb(usbId: String) =
        withContext(Dispatchers.IO) { albumRepository.getAllByUsbID(usbId) }

    suspend fun getAllSongFromAlbum(album: String) =
        withContext(Dispatchers.IO) { songRepository.getListSongFromAlbum(album) }

    suspend fun getAllArtistFromUsb(usbId: String) =
        withContext(Dispatchers.IO) { artistRepository.getAllByUsbID(usbId) }

    suspend fun getAllSongFromArtist(artist: String) =
        withContext(Dispatchers.IO) { songRepository.getListSongFromArtist(artist) }



    private suspend fun cleanupDatabase(newListSong: ArrayList<Song>) {
        val usbID = newListSong.firstOrNull()?.usbId ?: return
        Timber.d("*****************START CLEANUP DATABASE FOR USB: $usbID******************")
        //remove invalid song
        val oldSongsInDB = songRepository.getAllByUsbID(usbID)
        val newSongIDs = newListSong.map { it.mediaStoreId } as ArrayList<Long>
        val newSongPaths = newListSong.map { it.path } as ArrayList<String>
        val listSongToDelete: List<Song> =
            oldSongsInDB.filter { it.mediaStoreId !in newSongIDs || it.path !in newSongPaths }

        listSongToDelete.forEach {
            Timber.d("remove invalid song: ${it.title} ${it.path} ${it.mediaStoreId}")
            songRepository.removeSong(it.mediaStoreId)
        }

        //remove invalid album
        val oldAlbumsInDB = albumRepository.getAllByUsbID(usbID)
        val newAlbums = MusicUtil.splitIntoAlbums(newListSong)
//            val newAlbumIDs = newAlbums.map { it.id }
//            val listAlbumToDelete = oldAlbumsInDB.filter { it.id !in newAlbumIDs }.toMutableList()
//            listAlbumToDelete += newAlbums.filter { album -> newListSong.none { it.albumId == album.id } }

        val listAlbumToDelete: MutableList<Album> = arrayListOf()
        listAlbumToDelete += oldAlbumsInDB.filter { oldAlbum ->
            newAlbums.none { newAlbum -> newAlbum.title == oldAlbum.title }
        }
        for (album in listAlbumToDelete) {
            Timber.d("remove invalid album: ${album.title} ${album.id}")
            albumRepository.deleteAlbum(album.id)
        }


        //remove invalid artist
        val oldArtistInDB = artistRepository.getAllByUsbID(usbID)
        val newArtists = MusicUtil.splitIntoArtist(newListSong)
        Timber.d("newArtists: ${newArtists.size}")
        Timber.d("oldArtistInDB: ${oldArtistInDB.size}")

        val listArtistToDelete: MutableList<Artist> = arrayListOf()
        listArtistToDelete += oldArtistInDB.filter { oldArtist ->
            newArtists.none{ newArtist -> newArtist.title == oldArtist.title }
        }

//        for (artist in newArtists) {
//            val artistId = artist.id
//            val albumsByArtist = newAlbums.filter { it.artistId == artistId }
//            if (albumsByArtist.isEmpty()) {
//                listArtistToDelete.add(artist)
//                continue
//            }
//            // update album, track counts
//            val albumCount = albumsByArtist.size
//            val songCount = albumsByArtist.sumOf { it.songCount }
//            if (songCount != artist.songCount || albumCount != artist.albumCount) {
//                artistRepository.deleteArtist(artistId)
//                val updated = artist.copy(songCount = songCount, albumCount = albumCount)
//                artistRepository.insert(updated)
//            }
//        }
        listAlbumToDelete.forEach { artist ->
            artistRepository.deleteArtist(artist.id)
        }

        //remove invalid favorite
        val oldFavoriteInDB = favoriteRepository.getAllFavoriteByUsbID(usbID)
        val listFavoriteToDelete = oldFavoriteInDB.filter { it.pathSong !in newSongPaths }
        listFavoriteToDelete.forEach {
            Timber.d("remove invalid favorite: ${it.pathSong}")
            favoriteRepository.removeFavoriteSong(it.pathSong)
        }
        //update list favoriteSong
        updateSongFavorite(usbID)

        val songs = songRepository.getAllByUsbID(usbID)
        val favorites = favoriteRepository.getAllFavoriteByUsbID(usbID)

        Timber.e("Total after clean---- song: ${songs.size}, favorite: ${favorites.size}, artist: ${artistRepository.getAllByUsbID(usbID).size}")
        Timber.d("****************FINISH CLEANUP DATABASE FOR USB: $usbID****************")
    }

    private suspend fun updateSongFavorite(usbId: String) {
        val favoriteList = favoriteRepository.getAllFavoriteByUsbID(usbId)

        favoriteList.forEach { favorite ->
            val song = songRepository.getSongFromPath(favorite.pathSong)
            song.favorite = true
            songRepository.update(song)
        }
    }
}