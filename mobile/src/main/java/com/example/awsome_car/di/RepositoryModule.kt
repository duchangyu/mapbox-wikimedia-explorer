package com.example.awsome_car.di

import com.example.awsome_car.data.repository.WikimediaRepository
import com.example.awsome_car.domain.repository.ImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImageRepository(repository: WikimediaRepository): ImageRepository
}
