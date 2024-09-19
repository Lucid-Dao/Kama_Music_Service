package com.kanavi.automotive.kama.kama_music_service.di

import android.app.NotificationManager
import androidx.core.content.ContextCompat.getSystemService
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.MusicProvider
import org.koin.dsl.module

val musicModule = module {
    single { getSystemService(get(), NotificationManager::class.java) }
    single { MusicProvider(get()) }
}