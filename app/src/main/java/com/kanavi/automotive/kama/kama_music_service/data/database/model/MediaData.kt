package com.kanavi.automotive.kama.kama_music_service.data.database.model

data class MediaData(
    val usbId: String,
    val path: String,
    val parentFolder: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val lastModified: Long,
)