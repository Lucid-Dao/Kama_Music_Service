package com.kanavi.automotive.kama.kama_music_service.service.mediaSource

import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.IntDef
import com.kanavi.automotive.kama.kama_music_service.common.util.UsbUtil
import com.kanavi.automotive.kama.kama_music_service.data.database.model.album.Album
import com.kanavi.automotive.kama.kama_music_service.data.database.model.artist.Artist
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class UsbSource(
    private val rootPath: String,
) : AbstractMusicSource() {

    var songs: ArrayList<Song> = arrayListOf()
    //Folders
    var songMap: HashMap<Int, Song> = HashMap()
    var treeNode: TreeNode = TreeNode("")

    //Songs
    private val _songInDB = MutableStateFlow<List<Song>>(emptyList())
    val songInDB = _songInDB as StateFlow<List<Song>>

    //Albums
    private val _albumInDB = MutableStateFlow<List<Album>>(emptyList())
    val albumInDB = _albumInDB as StateFlow<List<Album>>

    private val _songInAlbumDB = HashMap<Long, List<Song>>()
    val songInAlbumDB = _songInAlbumDB

    //Artists
    private val _artistInDB = MutableStateFlow<List<Artist>>(emptyList())
    val artistInDB = _artistInDB as StateFlow<List<Artist>>

    private val _songInArtistDB = HashMap<Long, List<Song>>()
    val songInArtistDB = _songInArtistDB

    //Favorites
    private val _songFavoriteInDB = MutableStateFlow<List<Song>>(emptyList())
    val songFavoriteInDB = _songFavoriteInDB as StateFlow<List<Song>>

    fun favoriteInDB(): MutableStateFlow<List<Song>> {
        return _songFavoriteInDB
    }

    override suspend fun load() {

            UsbUtil.scanAllMusicFromUsb(
                rootPath,
                emptyList(),
                songs,
                songMap,
                treeNode,
                _songInDB,
                _albumInDB,
                _artistInDB,
                _songFavoriteInDB,
                _songInAlbumDB,
                _songInArtistDB
            )
    }

    override fun iterator(): Iterator<Song> {
        return (songs.clone() as ArrayList<Song>).iterator()
    }
}

/**
 * Interface used by [MusicService] for looking up [MediaMetadataCompat] objects.
 *
 * Because Kotlin provides methods such as [Iterable.find] and [Iterable.filter],
 * this is a convenient interface to have on sources.
 */
interface MusicSource : Iterable<Song> {

    /**
     * Begins loading the data for this music source.
     */
    suspend fun load()

    /**
     * Method which will perform a given action after this [MusicSource] is ready to be used.
     *
     * @param performAction A lambda expression to be called with a boolean parameter when
     * the source is ready. `true` indicates the source was successfully prepared, `false`
     * indicates an error occurred.
     */
    fun whenReady(performAction: (Boolean) -> Unit): Boolean
}

@IntDef(
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class State

/**
 * State indicating the source was created, but no initialization has performed.
 */
const val STATE_CREATED = 1

/**
 * State indicating initialization of the source is in progress.
 */
const val STATE_INITIALIZING = 2

/**
 * State indicating the source has been initialized and is ready to be used.
 */
const val STATE_INITIALIZED = 3

/**
 * State indicating an error has occurred.
 */
const val STATE_ERROR = 4

const val DEFAULT_CONNECT_SHARE_NAME = "Android"

/**
 * Base class for music sources
 */
abstract class AbstractMusicSource : MusicSource {
    @State
    var state: Int = STATE_CREATED
        set(value) {
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()


    /**
     * Performs an action when this MusicSource is ready.
     *
     * This method is *not* threadsafe. Ensure actions and state changes are only performed
     * on a single thread.
     */
    override fun whenReady(performAction: (Boolean) -> Unit): Boolean =
        when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                Timber.d("whenReady: state == STATE_CREATED || state == STATE_INITIALIZING")
                onReadyListeners += performAction
                false
            }

            else -> {
                Timber.d("whenReady: state == STATE_INITIALIZED")
                performAction(state != STATE_ERROR)
                true
            }
        }
}