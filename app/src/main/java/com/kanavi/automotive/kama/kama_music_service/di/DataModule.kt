package com.kanavi.automotive.kama.kama_music_service.di

import androidx.room.Room
import com.kanavi.automotive.kama.kama_music_service.common.constant.DatabaseEntry
import com.kanavi.automotive.kama.kama_music_service.data.database.UsbMusicDatabase
import com.kanavi.automotive.kama.kama_music_service.data.repository.AlbumRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.ArtistRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.FavoriteRepository
import com.kanavi.automotive.kama.kama_music_service.data.repository.SongRepository
import com.kanavi.automotive.kama.kama_music_service.service.mediaSource.PersistentStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    single { PersistentStorage(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            UsbMusicDatabase::class.java,
            DatabaseEntry.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }


    single { get<UsbMusicDatabase>().songDAO() }
    single { get<UsbMusicDatabase>().albumDAO() }
    single { get<UsbMusicDatabase>().artistDAO() }

    single { get<UsbMusicDatabase>().favoriteDao() }

    single { SongRepository(get()) }
    single { AlbumRepository(get()) }
    single { ArtistRepository(get()) }

    single { FavoriteRepository(get()) }

}