package com.kanavi.automotive.kama.kama_music_service.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.media.MediaBrowserServiceCompat
import com.kanavi.automotive.kama.kama_music_service.R
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.KEY_USB_ATTACHED
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_ATTACHED_STATE
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_FAVORITE
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_MUSICS_BY_SONGS
import com.kanavi.automotive.kama.kama_music_service.common.constant.MediaConstant.MEDIA_ID_RECENT_ROOT
import com.kanavi.automotive.kama.kama_music_service.common.extension.toMediaSessionQueue
import com.kanavi.automotive.kama.kama_music_service.common.util.DBHelper
import com.kanavi.automotive.kama.kama_music_service.common.util.ShuffleHelper.makeShuffleList
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song
import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song.Companion.emptySong
import com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback.MediaSessionCallback
import com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback.Playback
import com.kanavi.automotive.kama.kama_music_service.service.mediaPlayback.PlaybackManager
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MediaIDHelper
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MusicProvider
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.PersistentStorage
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.UsbSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Objects
import kotlin.math.min

class MusicService : MediaBrowserServiceCompat(), Playback.PlaybackCallbacks, KoinComponent {

    private val musicBind: IBinder = MusicBinder()

    private val notificationManager: NotificationManager by inject()
    private val musicProvider: MusicProvider by inject()
    private val storage: PersistentStorage by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Main)

    private lateinit var playbackManager: PlaybackManager
    private var mediaSession: MediaSessionCompat? = null

    private var trackEndedByCrossFade = false

    private var originalPlayingQueue = ArrayList<Song>()

    private var isUsbAttached: Boolean = false

    val isPlaying: Boolean
        get() = playbackManager.isPlaying

    @JvmField
    var playingQueue = ArrayList<Song>()

    @JvmField
    var position = -1
    private fun setPosition(position: Int) {
        openTrackAndPrepareNextAt(position) { success ->
            if (success) {
                notifyChange(PLAY_STATE_CHANGED)
            }
        }
    }

    private fun getPosition(): Int {
        return position
    }

    private fun getPreviousPosition(force: Boolean): Int {
        var newPosition = getPosition() - 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (newPosition < 0) {
                newPosition = playingQueue.size - 1
            }

            REPEAT_MODE_THIS -> if (force) {
                if (newPosition < 0) {
                    newPosition = playingQueue.size - 1
                }
            } else {
                newPosition = getPosition()
            }

            REPEAT_MODE_NONE -> if (newPosition < 0) {
                newPosition = 0
            }

            else -> if (newPosition < 0) {
                newPosition = 0
            }
        }
        return newPosition
    }

    @JvmField
    var nextPosition = -1
    private fun getNextPosition(force: Boolean): Int {
        var position = getPosition() + 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (isLastTrack) {
                position = 0
            }

            REPEAT_MODE_THIS -> if (force) {
                if (isLastTrack) {
                    position = 0
                }
            } else {
                position -= 1
            }

            REPEAT_MODE_NONE -> if (isLastTrack) {
                position -= 1
            }

            else -> if (isLastTrack) {
                position -= 1
            }
        }
        return position
    }

    val currentSong: Song
        get() = getSongAt(getPosition())

    val nextSong: Song?
        get() = if (isLastTrack && repeatMode == REPEAT_MODE_NONE) {
            null
        } else {
            getSongAt(getNextPosition(false))
        }

    private val isLastTrack: Boolean
        get() = getPosition() == playingQueue.size - 1

    val songDurationMillis: Int
        get() = playbackManager.songDurationMillis

    val songProgressMillis: Int
        get() = playbackManager.songProgressMillis

    //shuffleMode
    @JvmField
    var shuffleMode = 0

    fun getShuffleMode(): Int {
        return shuffleMode
    }

    fun setShuffleMode(shuffleMode: Int) {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_SHUFFLE_MODE, shuffleMode)
        }
        when (shuffleMode) {
            SHUFFLE_MODE_SHUFFLE -> {
                this.shuffleMode = shuffleMode
                makeShuffleList(playingQueue, getPosition())
                position = 0
            }

            SHUFFLE_MODE_NONE -> {
                this.shuffleMode = shuffleMode
                val currentSongId = Objects.requireNonNull(currentSong).id
                playingQueue = ArrayList(originalPlayingQueue)
                var newPosition = 0
                for (song in playingQueue) {
                    if (song.id == currentSongId) {
                        newPosition = playingQueue.indexOf(song)
                    }
                }
                position = newPosition
            }
        }
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        notifyChange(QUEUE_CHANGED)
    }

    fun toggleShuffle() {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            setShuffleMode(SHUFFLE_MODE_SHUFFLE)
        } else {
            setShuffleMode(SHUFFLE_MODE_NONE)
        }
    }

    //repeat mode
    var repeatMode = REPEAT_MODE_NONE
        private set(value) {
            when (value) {
                REPEAT_MODE_NONE, REPEAT_MODE_ALL, REPEAT_MODE_THIS -> {
                    field = value
                    PreferenceManager.getDefaultSharedPreferences(this).edit {
                        putInt(SAVED_REPEAT_MODE, value)
                    }
                    prepareNext()
                    handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
                }
            }
        }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            REPEAT_MODE_NONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_THIS
            else -> REPEAT_MODE_NONE
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")

        scanUsbAndHandleIfNeeded()

        val powerManager = getSystemService<PowerManager>()
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        }
        wakeLock?.setReferenceCounted(false)


        registerUsbEventReceiver()
        setupPlaybackManager()
        setupMediaSession(isActive = false)
        setErrorPlaybackState()
        initNotification()

//        restoreState()
        sessionToken = mediaSession?.sessionToken
        musicProvider.setMusicService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        if (intent != null && intent.action != null) {
            serviceScope.launch {
                restoreQueuesAndPositionIfNecessary()
                when (intent.action) {
                    ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                        pause()
                    } else {
                        play()
                    }

                    ACTION_PAUSE -> pause()
                    ACTION_PLAY -> play()
                    ACTION_REWIND -> back(true)
                    ACTION_SKIP -> playNextSong(true)
                }
            }
        }
        return START_STICKY
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        /**
         * By default return the browsable root. Treat the EXTRA_RECENT flag as a special case
         * and return the recent root instead.
         */
        Timber.d("onGetRoot: clientPackageName = $clientPackageName, clientUid = $clientUid, rootHints = $rootHints")

        val isRecentRequest = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        val browserRootPath = if (isRecentRequest) MEDIA_ID_RECENT_ROOT else MEDIA_ID_ATTACHED_STATE
        return BrowserRoot(browserRootPath, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem?>>
    ) {
        Timber.d("parentID: $parentId")

        when (parentId) {
            MEDIA_ID_RECENT_ROOT -> {
                Timber.d("items __MUSIC_RECENT__")
                //load D -> sendResult
                // load files....
                result.sendResult(storage.loadRecentSong()?.let { song -> listOf(song) })

            }

            MEDIA_ID_ATTACHED_STATE -> {
                Timber.d("load items for __MUSIC_USB_ATTACHED__")
                updateMediaSessionPlaybackState()
                val isUsbAttach = isUsbAttached

                val bundle = Bundle().apply {
                    putBoolean(KEY_USB_ATTACHED, isUsbAttach)
                }

                val mediaItemWithBundle = MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(parentId)
                        .setExtras(bundle)
                        .build(),
                    MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
                result.sendResult(mutableListOf(mediaItemWithBundle))
            }

            else -> {
                Timber.d("load items for: $parentId")
                result.sendResult(musicProvider.getChildren(parentId))
            }
        }
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>,
        options: Bundle
    ) {
        when(parentId){
            MEDIA_ID_MUSICS_BY_SONGS ->
            {
                result.detach()
                val page = options.getInt(MediaBrowserCompat.EXTRA_PAGE)
                val pageSize = options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE)

                val mediaItems = musicProvider.getChildren(parentId)
                val fromIndex = page * pageSize
                val toIndex = min(fromIndex + pageSize, mediaItems.size)

                Timber.d("fromIndex: $fromIndex, toIndex: $toIndex")
                if (fromIndex < toIndex) {
                    val itemsForPage = mediaItems.subList(fromIndex, toIndex)
                    result.sendResult(itemsForPage)
                } else {
                    result.sendResult(emptyList())
                }
            } else ->{
                Timber.d("Don't support")
            }
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        Timber.d("action is: $action")
        when(action){
            ADD_FAVORITE -> {
                result.detach()
                val path = extras?.getString(EXTRA_SONG_PATH)
                val isFavorite = extras?.getBoolean(EXTRA_FAVORITE_ENABLE)
                Timber.d("path song favorite: $path, isFavorite: $isFavorite")
                if (path != null) {
                    try {
                        CoroutineScope(Main).launch {
                            val favorite = withContext(IO) {
                                DBHelper.updateDataFavorite(path, isFavorite?: false)
                            }
                            musicProvider.getUsbSource()?.favoriteInDB()?.value = favorite.second!!
                            val bundle = Bundle()
                            Timber.d("favorite in song: $favorite")
                            bundle.putBoolean(EXTRA_SONG_PATH, favorite.first)
                            result.sendResult(bundle)

                            musicProvider.notifyDataChanged(
                                listOf(MEDIA_ID_MUSICS_BY_FAVORITE)
                            )
                        }
                    }catch (e: Exception){
                        result.sendError(null)
                    }
                }
            }
            else -> super.onCustomAction(action, extras, result)
        }
    }

    fun openQueue(
        playingQueue: List<Song>?,
        startPosition: Int,
        startPlaying: Boolean,
    ) {
        if (!playingQueue.isNullOrEmpty()
            && startPosition >= 0 && startPosition < playingQueue.size
        ) {
            Timber.d("openQueue: queuesize: ${playingQueue.size} startPosition: $startPosition, startPlaying: $startPlaying")
            // it is important to copy the playing queue here first as we might add/remove songs later
            originalPlayingQueue = ArrayList(playingQueue)
            this.playingQueue = ArrayList(originalPlayingQueue)
            var position = startPosition
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                makeShuffleList(this.playingQueue, startPosition)
                position = 0
            }
            if (startPlaying) {
                playSongAt(position)
            } else {
                setPosition(position)
            }
            notifyChange(QUEUE_CHANGED)
        }
    }

    @Synchronized
    private fun openCurrent(completion: (success: Boolean) -> Unit) {
        val force = if (!trackEndedByCrossFade) {
            true
        } else {
            trackEndedByCrossFade = false
            false
        }
        currentSong.let {
            playbackManager.setDataSource(it, force) { success ->
                completion(success)
            }
        }
    }


    @Synchronized
    fun openTrackAndPrepareNextAt(position: Int, completion: (success: Boolean) -> Unit) {
        this.position = position
        openCurrent { success ->
            completion(success)
            if (success) {
                prepareNextImpl()
            }
            notifyChange(META_CHANGED)
        }
    }

    @Synchronized
    fun play() {
        playbackManager.play { playSongAt(getPosition()) }
        handleChangeInternal(META_CHANGED)
        notifyChange(PLAY_STATE_CHANGED)
    }

    fun playNextSong(force: Boolean) {
        playSongAt(getNextPosition(force))
    }

    fun playPreviousSong(force: Boolean) {
        playSongAt(getPreviousPosition(force))
    }

    fun playSongAt(position: Int) {
        // Every chromecast method needs to run on main thread or you are greeted with IllegalStateException
        // So it will use Main dispatcher
        // And by using Default dispatcher for local playback we are reduce the burden of main thread
        serviceScope.launch(Dispatchers.Default) {
            openTrackAndPrepareNextAt(position) { success ->
                if (success) {
                    play()
                } else {
                    Timber.e("Failed to open track at position $position")
                }
            }
        }
    }

    fun back(force: Boolean) {
        if (songProgressMillis > 2000) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }

    fun pause(force: Boolean = false) {
        playbackManager.pause(force) {
            notifyChange(PLAY_STATE_CHANGED)
        }
    }

    @Synchronized
    fun seek(millis: Int, force: Boolean = true): Int {
        return try {
            val newPosition = playbackManager.seek(millis, force)
            newPosition
        } catch (e: Exception) {
            -1
        }
    }

    private fun savePosition() {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_POSITION, getPosition())
        }
    }

    fun savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_POSITION_IN_TRACK, songProgressMillis)
        }
    }

    private fun prepareNext() {
        prepareNextImpl()
    }

    @Synchronized
    fun prepareNextImpl() {
        try {
            val nextPosition = getNextPosition(false)
            val nextSong = getSongAt(nextPosition)
            Timber.d("prepareNextImpl: title: ${nextSong.title}, uri: ${nextSong.getUri()} path: ${nextSong.path}")
            playbackManager.setNextDataSource(getSongAt(nextPosition).path)
            this.nextPosition = nextPosition
        } catch (ignored: Exception) {
        }
    }


    private fun getSongAt(position: Int): Song {
        return if ((position >= 0) && (position < playingQueue.size)) {
            playingQueue[position]
        } else {
            emptySong
        }
    }

    private fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
    }

    private fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun sendChangeInternal(what: String) {
    }

    private fun handleChangeInternal(what: String) {
        when (what) {
            PLAY_STATE_CHANGED -> {
                updateMediaSessionPlaybackState()
                val isPlaying = isPlaying
                if (!isPlaying && songProgressMillis > 0) {
                    savePositionInTrack()
                }
            }

            META_CHANGED -> {
                updateMediaSessionMetaData(::updateMediaSessionPlaybackState)
                savePosition()
                savePositionInTrack()
                serviceScope.launch(IO) {
                    val currentSong = currentSong
                    storage.saveRecentSong(currentSong, songProgressMillis.toLong())
                }
            }

            QUEUE_CHANGED -> {
                mediaSession?.setQueueTitle("Now playing queue")
                mediaSession?.setQueue(playingQueue.toMediaSessionQueue())
                updateMediaSessionMetaData(::updateMediaSessionPlaybackState) // because playing queue size might have changed
//                saveQueues()
                if (playingQueue.size > 0) {
                    prepareNext()
                } else {
//                    stopForegroundAndNotification
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        mediaSession?.isActive = false
        quit()
        releaseResource()
        unregisterUsbEventReceiver()
        wakeLock?.release()
    }

    fun quit() {
        pause()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun acquireWakeLock() {
        wakeLock?.acquire(30000)
    }

    private fun releaseWakeLock() {
        if (wakeLock!!.isHeld) {
            wakeLock?.release()
        }
    }

    private fun releaseResource() {
        playbackManager?.release()
        mediaSession?.release()
    }

    private fun setupPlaybackManager() {
        if (!::playbackManager.isInitialized) {
            Timber.d("setupPlaybackManager")
            playbackManager = PlaybackManager(this)
            playbackManager.setCallbacks(this)
            playbackManager.setMusicService(this)
        }
    }

    private fun setupMediaSession(isActive: Boolean = false) {
        if (mediaSession == null) {
            Timber.d("setupMediaSession")
            mediaSession = MediaSessionCompat(this, PACKAGE_NAME)
            val mediaSessionCallback = MediaSessionCallback(this)
            mediaSession?.setCallback(mediaSessionCallback)
        }
        mediaSession?.isActive = isActive
    }

    fun restoreState(completion: () -> Unit = {}) {
        //restore shuffleMode, repeatMode, queue and position...
        shuffleMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_SHUFFLE_MODE, SHUFFLE_MODE_NONE
        )
        repeatMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_REPEAT_MODE, REPEAT_MODE_NONE
        )
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
        serviceScope.launch {
            restoreQueuesAndPositionIfNecessary()
            completion()
        }
    }

    private fun restoreQueuesAndPositionIfNecessary() {

    }

    private fun initNotification() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Music Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(notificationChannel)
        val notification: Notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        ).setSmallIcon(R.drawable.ic_launcher_background).setOnlyAlertOnce(true).build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onTrackEnded() {
        acquireWakeLock()
        if (repeatMode == REPEAT_MODE_NONE && isLastTrack) {
            notifyChange(PLAY_STATE_CHANGED)
            seek(0, false)
        } else {
            playNextSong(false)
        }
        releaseWakeLock()
    }


    override fun onTrackEndedWithCrossfade() {
        trackEndedByCrossFade = true
        onTrackEnded()
    }

    override fun onTrackWentToNext() {
        if (repeatMode == REPEAT_MODE_NONE && isLastTrack) {
            playbackManager.setNextDataSource(null)
            pause(false)
            seek(0, false)
        } else {
            position = nextPosition
            prepareNextImpl()
            notifyChange(META_CHANGED)
        }
    }

    fun updateFavoriteChange(path: String, category: String) {
//        DBHelper.updateDataFavorite(path)
//        updateMediaSessionMetaData({}, category)
    }

    @SuppressLint("CheckResult")
    fun updateMediaSessionMetaData(
        onCompletion: () -> Unit,
        category: String = MediaConstant.MEDIA_ID_MUSICS_BY_FILE
    ) {
        Timber.d("onResourceReady: ")

        val song = currentSong

        if (song.id == -1L) {
            mediaSession?.setMetadata(null)
            return
        }
        val songID = MediaIDHelper.createMediaID(
            song.path,
            category
        )

        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, songID)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.path)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                (getPosition() + 1).toLong()
            )
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            //consider load cover art and put in here
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playingQueue.size.toLong())
            //favorite
            .putLong(METADATA_KEY_FAVORITE, if (song.favorite) 1 else 0)

        mediaSession?.setMetadata(metaData.build())
        onCompletion()
    }

    fun updateMediaSessionPlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(MEDIA_SESSION_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                songProgressMillis.toLong(),
                1f
            )
        setCustomAction(stateBuilder)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CYCLE_REPEAT, repeatMode.toString(), R.drawable.ic_launcher_background
            )
                .build()
        )

        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                TOGGLE_SHUFFLE, shuffleMode.toString(), R.drawable.ic_launcher_background
            )
                .build()
        )

        //Add favorite
    }

    override fun onPlayStateChanged() {
        notifyChange(PLAY_STATE_CHANGED)
    }

    private fun registerUsbEventReceiver() {
        if (!usbReceiverRegistered) {
            Timber.d("ReceiverRegistered is Success")

            registerReceiver(usbEventReceiver, usbReceiverIntentFilter)
            usbReceiverRegistered = true
        }
    }

    private fun unregisterUsbEventReceiver() {
        Timber.d("unregisterUsbEventReceiver")
        unregisterReceiver(usbEventReceiver)
        usbReceiverRegistered = false
    }

    private var usbReceiverRegistered = false

    private val usbReceiverIntentFilter = IntentFilter().apply {
        addAction(Intent.ACTION_MEDIA_CHECKING)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        addAction(Intent.ACTION_MEDIA_MOUNTED)
        addAction(Intent.ACTION_MEDIA_UNMOUNTED)
        addAction(Intent.ACTION_MEDIA_EJECT)
        addAction(Intent.ACTION_MEDIA_REMOVED)
    }

    private val usbEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("action: ${intent.action} data: ${intent.data}")
            when (intent.action) {

                Intent.ACTION_MEDIA_CHECKING -> {
                    Timber.d("ACTION_MEDIA_CHECKING")
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Timber.d("ACTION_USB_DEVICE_ATTACHED")
                    scanUsbAndHandleIfNeeded()
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Timber.d("ACTION_USB_DEVICE_DETACHED")
                    handleUsbDetachEvent()
                }

                Intent.ACTION_MEDIA_MOUNTED -> {
                    Timber.d("ACTION_MEDIA_MOUNTED")
                    val newMountedUsbID = intent.data.toString().drop(7)
                    Timber.d("newMountedUsbID: $newMountedUsbID")
                    handleUsbMounted(newMountedUsbID)
                    isUsbAttached = true
                    musicProvider.notifyDataChanged(listOf(MEDIA_ID_ATTACHED_STATE))
                }

                Intent.ACTION_MEDIA_UNMOUNTED -> {
                    Timber.d("ACTION_MEDIA_UNMOUNTED")
                    val unmountedUsbId = intent.data.toString().drop(7)
                    musicProvider.removeUsbSource(unmountedUsbId)
                    playbackManager.stop()
                    setErrorPlaybackState()
                    setupMediaSession(isActive = false)
                    isUsbAttached = false
                    musicProvider.notifyDataChanged(listOf(MEDIA_ID_ATTACHED_STATE))
                }

                Intent.ACTION_MEDIA_REMOVED -> {
                    Timber.d("ACTION_MEDIA_REMOVED")
                }

                Intent.ACTION_MEDIA_EJECT -> {
                    Timber.d("ACTION_MEDIA_EJECT")
                }
            }
        }
    }

    fun runShellCommand(command: String): List<String> {
        val process = Runtime.getRuntime().exec(command)
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream)
        )

        val usbList = mutableListOf<String>()
        val log = StringBuilder()
        var line: String?
        line = bufferedReader.readLine()
        while (line != null) {
            if (line.contains("media_rw")) {
                val start = line.indexOf("/mnt/media_rw")
                val end = line.indexOf("type")
                usbList.add(line.substring(start, end).trim())
            }

            line = bufferedReader.readLine()
        }
        val reader = BufferedReader(
            InputStreamReader(process.errorStream)
        )

        // if we had an error during ex we get here
        val errorLog = StringBuilder()
        var errorLine: String?
        errorLine = reader.readLine()
        while (errorLine != null) {
            errorLog.append(errorLine + "\n")
            errorLine = reader.readLine()
        }
        if (errorLog.toString() != "")
            Timber.i("command : $command $log error $errorLog")
        else
            Timber.i("command : $command $log")

        return usbList
    }

    private fun scanUsbAndHandleIfNeeded() {
        Timber.d("===========scanUsbAndHandleIfNeeded=========")
        var usbIsAdded = false
        Timber.d("=====START CHECKING: usbIsAdded = $usbIsAdded, usbID = ${musicProvider.getSelectedUsbID()}=====")
        serviceScope.launch(IO) {
            var count = 0
            while (!usbIsAdded) {
                val usbList = runShellCommand("mount")
                usbList.firstOrNull()?.let { usbId ->
                    Timber.d("Found USB device:$usbId => stop scan after $count times")
                    usbIsAdded = true
                    handleUsbMounted(usbId)
                }
                delay(200)
                count++
                if (count >= TIME_TO_TRY_SCAN_USB_DEVICE) {
                    Timber.d("Try to scan over $TIME_TO_TRY_SCAN_USB_DEVICE times and do not found any USB device is ADDED => stop scan")
                    break
                }
                if (usbIsAdded) {
                    setupPlaybackManager()
                    setupMediaSession(isActive = true)
                    break
                }
            }
        }
    }

    fun handleUsbMounted(newMountedUsbID: String){
        musicProvider.apply {
            setSelectedUsbID(newMountedUsbID)
            addUsbSource(newMountedUsbID, UsbSource(newMountedUsbID))
            getUsbSource()?.let {
                CoroutineScope(IO).launch {
                    it.load()
                }
                setupPlaybackManager()
            }
        }
    }

    private fun handleUsbDetachEvent() {
        Timber.d("==========handleUsbDetachEvent=========")
        var usbIsRemoved = false
        serviceScope.launch(IO) {
            var count = 0
            while (!usbIsRemoved) {
                val newUsbList = runShellCommand("mount")
                if (musicProvider.getSelectedUsbID() == newUsbList.first()) {
                    playbackManager.stop()
                    setupMediaSession(isActive = false)
                }
                musicProvider.removeUsbSource(musicProvider.getSelectedUsbID())
                usbIsRemoved = true
                delay(200)
                count++
                if (count >= TIME_TO_TRY_SCAN_USB_DEVICE) {
                    Timber.d("Try to scan over $TIME_TO_TRY_SCAN_USB_DEVICE times and do not found any USB device is REMOVED => stop scan")
                    break
                }
                if (usbIsRemoved) {
                    break
                }
            }
        }
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        // For Android auto, need to call super, or onGetRoot won't be called.
        return if ("android.media.browse.MediaBrowserService" == intent.action) {
            super.onBind(intent)!!
        } else musicBind
    }

    override fun onUnbind(intent: Intent): Boolean {
//        if (!isPlaying) {
//            stopSelf()
//        }
        return true
    }

    private fun setErrorPlaybackState() {
        val errorState = PlaybackStateCompat.Builder()
            .setErrorMessage(getString(R.string.STR_MMS_0358_ID))
            .setState(
                PlaybackStateCompat.STATE_ERROR,
                0, 0f
            ).build()
        mediaSession?.setPlaybackState(errorState)
    }

    companion object {
        private const val PACKAGE_NAME = "com.kanavi.automotive.kama.kama_music_service"
        const val NOTIFICATION_CHANNEL_ID = "$PACKAGE_NAME.NOTIFICATION_CHANNEL_ID"
        const val NOTIFICATION_ID = 2468
        const val TIME_TO_TRY_SCAN_USB_DEVICE = 68

        const val ACTION_GET_HOST_IP = "$PACKAGE_NAME.ACTION_GET_IP_ADDRESS"
        const val ACTION_TOGGLE_PAUSE = "$PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$PACKAGE_NAME.play"
        const val ACTION_PAUSE = "$PACKAGE_NAME.pause"
        const val ACTION_STOP = "$PACKAGE_NAME.stop"
        const val ACTION_SKIP = "$PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$PACKAGE_NAME.rewind"
        const val ACTION_QUIT = "$PACKAGE_NAME.quitservice"

        const val META_CHANGED = "$PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$PACKAGE_NAME.queuechanged"
        const val PLAY_STATE_CHANGED = "$PACKAGE_NAME.playstatechanged"
        const val REPEAT_MODE_CHANGED = "$PACKAGE_NAME.repeatmodechanged"
        const val SHUFFLE_MODE_CHANGED = "$PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$PACKAGE_NAME.mediastorechanged"
        const val CYCLE_REPEAT = "$PACKAGE_NAME.cyclerepeat"
        const val TOGGLE_SHUFFLE = "$PACKAGE_NAME.toggleshuffle"
        const val SAVED_POSITION = "$PACKAGE_NAME.POSITION"
        const val SAVED_POSITION_IN_TRACK = "$PACKAGE_NAME.POSITION_IN_TRACK"
        const val SAVED_SHUFFLE_MODE = "$PACKAGE_NAME.SHUFFLE_MODE"
        const val SAVED_REPEAT_MODE = "$PACKAGE_NAME.REPEAT_MODE"
        const val SHUFFLE_MODE_NONE = 0
        const val SHUFFLE_MODE_SHUFFLE = 1
        const val REPEAT_MODE_NONE = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_THIS = 2

        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)

        //ADD FAVORITE
        private const val ADD_FAVORITE = "$PACKAGE_NAME.ADD_FAVORITE"
        private const val METADATA_KEY_FAVORITE = "KEY_FAVORITE"
        private const val EXTRA_FAVORITE_ENABLE = "FAVORITE_ENABLE"
        private const val EXTRA_SONG_PATH = "SONG_PATH"

    }
}