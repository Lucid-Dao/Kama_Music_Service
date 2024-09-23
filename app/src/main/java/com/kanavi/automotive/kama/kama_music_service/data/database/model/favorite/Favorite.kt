package com.kanavi.automotive.kama.kama_music_service.data.database.model.favorite

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.FAVORITE_ID
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.FAVORITE_PATH_SONG
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.FAVORITE_TABLE_NAME
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry.USB_ID

@Entity(
    tableName = FAVORITE_TABLE_NAME,
    indices = [Index(value = [FAVORITE_PATH_SONG], unique = true)]
)
data class Favorite(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = FAVORITE_ID)
    var id: Long,
    @ColumnInfo(name = FAVORITE_PATH_SONG)
    val pathSong: String,
    @ColumnInfo(name = USB_ID)
    var usbId: String = "",

    )
