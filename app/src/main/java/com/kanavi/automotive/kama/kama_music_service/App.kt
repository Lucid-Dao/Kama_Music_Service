package com.kanavi.automotive.kama.kama_music_service

import android.app.Application
import com.kanavi.automotive.kama.kama_music_service.di.dataModule
import com.kanavi.automotive.kama.kama_music_service.di.musicModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        app = this

        Timber.plant(TimberDebugTree())
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                listOf(
//                    appModule,
                    dataModule,
                    musicModule
                )
            )
        }
    }

    companion object {
        lateinit var app: App
    }
}