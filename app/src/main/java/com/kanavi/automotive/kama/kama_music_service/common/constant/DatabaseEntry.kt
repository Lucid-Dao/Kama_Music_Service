package com.kanavi.automotive.kama.kama_music_service.common.constant

object DatabaseEntry {
    const val DATABASE_NAME = "usb_music_database.db"
    const val DATABASE_VERSION = 1

    //USB
    const val USB_ID = "usb_id"

    //Song
    const val SONG_TABLE_NAME = "song"
    const val SONG_ID = "song_id"
    const val SONG_MEDIA_STORE_ID = "song_media_store_id"
    const val SONG_TITLE = "song_title"
    const val SONG_ARTIST = "song_artist"
    const val SONG_PATH = "song_path"
    const val SONG_DURATION = "song_duration"
    const val SONG_ALBUM = "song_album"
    const val SONG_COVER_ART = "song_cover_art"
    const val SONG_FOLDER_NAME = "song_folder_name"
    const val SONG_ALBUM_ID = "song_album_id"
    const val SONG_ARTIST_ID = "song_artist_id"
    const val SONG_YEAR = "song_year"
    const val SONG_DATE_ADDED = "song_date_added"
    const val SONG_FAVORITE = "song_favorite"

    //Album
    const val ALBUM_TABLE_NAME = "album"
    const val ALBUM_ID = "album_id"
    const val ALBUM_ARTIST = "album_artist"
    const val ALBUM_SONG_COUNT = "album_song_count"
    const val ALBUM_TITLE = "album_title"
    const val ALBUM_COVER_ART = "album_cover_art"
    const val ALBUM_YEAR = "album_year"
    const val ALBUM_ARTIST_ID = "album_artist_id"
    const val ALBUM_DATE_ADDED = "album_date_added"

    //Artist
    const val ARTIST_TABLE_NAME = "artist"
    const val ARTIST_ID = "artist_id"
    const val ARTIST_TITLE = "artist_title"
    const val ARTIST_ALBUM_COUNT = "artist_album_count"
    const val ARTIST_SONG_COUNT = "artist_song_count"
    const val ARTIST_COVER_ART = "artist_cover_art"

    //Favorite
    const val FAVORITE_TABLE_NAME = "favorite"
    const val FAVORITE_ID = "favorite_id"
    const val FAVORITE_PATH_SONG = "favorite_song_path"

}