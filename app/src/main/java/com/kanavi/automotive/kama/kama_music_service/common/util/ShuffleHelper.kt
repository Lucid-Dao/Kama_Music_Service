package com.kanavi.automotive.kama.kama_music_service.common.util

import com.kanavi.automotive.kama.kama_music_service.data.database.model.song.Song


object ShuffleHelper {

    fun makeShuffleList(listToShuffle: MutableList<Song>, current: Int) {
        if (listToShuffle.isEmpty()) return
        if (current >= 0) {
            val song = listToShuffle.removeAt(current)
            listToShuffle.shuffle()
            listToShuffle.add(0, song)
        } else {
            listToShuffle.shuffle()
        }
    }
}
