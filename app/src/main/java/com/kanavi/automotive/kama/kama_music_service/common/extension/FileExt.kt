package com.kanavi.automotive.kama.kama_music_service.common.extension

import android.content.Context
import com.kanavi.automotive.kama.kama_music_service.data.database.model.FileDirItem
import com.kanavi.automotive.kama.kama_music_service.common.constant.Constants.NOMEDIA
import java.io.File


//fun File.containMusicFile(): Boolean {
//    if (this.isHidden) {
//        return false
//    }
//    if (!this.containsNoMedia()) {
//        this.listFiles().orEmpty().forEach { child ->
//            val audioFilePaths = arrayListOf<String>()
//            UsbUtil.findAudioFiles(child, audioFilePaths, emptyList())
//            if (audioFilePaths.isNotEmpty()) {
//                return true
//            }
//        }
//    }
//    return false
//}

fun File.containsNoMedia(): Boolean {
    return if (!isDirectory) {
        false
    } else {
        File(this, NOMEDIA).exists()
    }
}

fun File.getProperSize(countHiddenItems: Boolean): Long {
    return if (isDirectory) {
        getDirectorySize(this, countHiddenItems)
    } else {
        length()
    }
}

fun File.getFileCount(countHiddenItems: Boolean): Int {
    return if (isDirectory) {
        getDirectoryFileCount(this, countHiddenItems)
    } else {
        1
    }
}

private fun getDirectoryFileCount(dir: File, countHiddenItems: Boolean): Int {
    var count = -1
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            count++
            for (i in files.indices) {
                val file = files[i]
                if (file.isDirectory) {
                    count++
                    count += getDirectoryFileCount(file, countHiddenItems)
                } else if (!file.name.startsWith('.') || countHiddenItems) {
                    count++
                }
            }
        }
    }
    return count
}

private fun getDirectorySize(dir: File, countHiddenItems: Boolean): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i], countHiddenItems)
                } else if (!files[i].name.startsWith('.') && !dir.name.startsWith('.') || countHiddenItems) {
                    size += files[i].length()
                }
            }
        }
    }
    return size
}


fun File.getDirectChildrenCount(context: Context, countHiddenItems: Boolean): Int {
    val fileCount = listFiles()?.filter {
        if (countHiddenItems) {
            true
        } else {
            !it.name.startsWith('.')
        }
    }?.size ?: 0


    return fileCount
}

fun File.toFileDirItem(context: Context) = FileDirItem(
    absolutePath,
    name,
    context.getIsPathDirectory(absolutePath),
    0,
    length(),
    lastModified()
)
