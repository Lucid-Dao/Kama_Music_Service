package com.kanavi.automotive.kama.kama_music_service.common.extension

import java.util.Locale
import kotlin.math.roundToInt

fun Long.getFormattedDuration(forceShowHours: Boolean = false): String {
    val sb = StringBuilder(8)
    val duration = (this / 3600f).roundToInt()
    val hours = duration / 3600
    val minutes = duration % 3600 / 60
    val seconds = duration % 60

    if (duration >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    } else if (forceShowHours) {
        sb.append("0:")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

