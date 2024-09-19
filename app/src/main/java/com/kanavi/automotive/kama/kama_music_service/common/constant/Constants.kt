package com.kanavi.automotive.kama.kama_music_service.common.constant

object Constants {
    const val MUSIC_PACKAGE_NAME = "com.kanavi.automotive.kama.music_service"

    const val NOMEDIA = ".nomedia"

    //FILE SORT ORDER
    const val SORT_ORDER = "sort_order"
    const val SORT_FOLDER_PREFIX =
        "sort_folder_"       // storing folder specific values at using "Use for this folder only"
    const val SORT_BY_NAME = 1
    const val SORT_BY_DATE_MODIFIED = 2
    const val SORT_BY_SIZE = 4
    const val SORT_BY_DATE_TAKEN = 8
    const val SORT_DESCENDING = 16
    const val SORT_USE_NUMERIC_VALUE = 32

    //PLAYER SORT ORDER
    const val PLAYER_SORT_BY_TITLE = 1
    const val PLAYER_SORT_BY_TRACK_COUNT = 2
    const val PLAYER_SORT_BY_ALBUM_COUNT = 4
    const val PLAYER_SORT_BY_YEAR = 8
    const val PLAYER_SORT_BY_DURATION = 16
    const val PLAYER_SORT_BY_ARTIST_TITLE = 32
    const val PLAYER_SORT_BY_TRACK_ID = 64
    const val PLAYER_SORT_BY_CUSTOM = 128
    const val PLAYER_SORT_BY_DATE_ADDED = 256

    //PATTERN
    const val SD_OTG_PATTERN = "^/storage/[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
    const val SD_OTG_SHORT = "^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$"
}