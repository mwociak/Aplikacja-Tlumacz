package com.mcodeproject.di

import android.content.Context
import com.mcodeproject.data.repository.SettingsRepository
import com.mcodeproject.data.repository.TranslationRepository
import com.mcodeproject.util.NetworkObserver
import com.mcodeproject.util.TtsHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkObserver(
        @ApplicationContext context: Context
    ): NetworkObserver {
        return NetworkObserver(context)
    }

    @Provides
    @Singleton
    fun provideTranslationRepository(
        networkObserver: NetworkObserver
    ): TranslationRepository {
        return TranslationRepository(networkObserver)
    }

    @Provides
    @Singleton
    fun provideTtsHelper(
        @ApplicationContext context: Context
    ): TtsHelper {
        return TtsHelper(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
}
