package com.kanavi.automotive.kama.kama_music_service.common.util

import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite.Favorite
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.data.repository.AlbumRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.ArtistRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.FavoriteRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

object DBHelper : KoinComponent {
    private val songRepository: SongRepository by inject()
    private val albumRepository: AlbumRepository by inject()

    private val favoriteRepository: FavoriteRepository by inject()

    suspend fun updateAllDatabase(listSong: ArrayList<Song>) {

        songRepository.insertAll(listSong)

        val newAlbums = MusicUtil.splitIntoAlbums(listSong)
        albumRepository.insertAll(newAlbums)

        cleanupDatabase(listSong)
    }


    suspend fun updateDataFavorite(path: String, isFavorite: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val song = songRepository.getSongFromPath(path)

            if (song.favorite) {
                val favorite = Favorite(id = 0, pathSong = path, usbId = song.usbId)
                favoriteRepository.insert(favorite)
            } else {
                favoriteRepository.removeFavoriteSong(path)
            }
            song.favorite = isFavorite
            songRepository.update(song)
            Timber.d("favorite size: ${favoriteRepository.getAllFavoriteByUsbID(song.usbId).size}")

            return@withContext song.favorite
        }

    }

    suspend fun getAllSongFromAlbum(album: String) = withContext(Dispatchers.IO) { songRepository.getListSongFromAlbum(album)}

    suspend fun getAllSong() = withContext(Dispatchers.IO){ songRepository.getAll()}

    suspend fun getAllSongFavorite(usbId: String) = withContext(Dispatchers.IO){ songRepository.getAllSongFavorite(usbId) }

    suspend fun getAllSongFromUsb(usbId: String) =  withContext(Dispatchers.IO){ songRepository.getAllByUsbID(usbId) }

    suspend fun getSongFromPath(path: String) = withContext(Dispatchers.IO){ songRepository.getSongFromPath(path) }

    suspend fun getAllAlbumFromUsb(usbId: String) = withContext(Dispatchers.IO){ albumRepository.getAllByUsbID(usbId)}

    suspend fun getAllAlbum() = withContext(Dispatchers.IO){ albumRepository.getAll() }

    private fun cleanupDatabase(newListSong: ArrayList<Song>) =
        CoroutineScope(Dispatchers.IO).launch {
            val usbID = newListSong.firstOrNull()?.usbId ?: return@launch
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
                newAlbums.none { newAlbum -> newAlbum.title == oldAlbum.title}
            }
            for (album in listAlbumToDelete) {
                Timber.d("remove invalid album: ${album.title} ${album.id}")
                albumRepository.deleteAlbum(album.id)
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

            Timber.e("Total after clean---- song: ${songs.size}, favorite: ${favorites.size}")
            Timber.d("****************FINISH CLEANUP DATABASE FOR USB: $usbID****************")
        }

    private suspend fun updateSongFavorite(usbId: String) {
        val favoriteList = favoriteRepository.getAllFavoriteByUsbID(usbId)
        Timber.e("usbId: $usbId, pathFavorite: ${favoriteList.size}")

        favoriteList.forEach { favorite ->
            val song = songRepository.getSongFromPath(favorite.pathSong)
            song.favorite = true
            songRepository.update(song)
        }
    }
}