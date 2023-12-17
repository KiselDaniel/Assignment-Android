package com.goodrequest.hiring.di

import com.goodrequest.hiring.network.PokemonApi
import com.goodrequest.hiring.repository.PokemonRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun providePokemonApi(): PokemonApi {
        return PokemonApi
    }

    @Provides
    fun providePokemonRepository(pokemonApi: PokemonApi): PokemonRepository {
        return PokemonRepository(pokemonApi)
    }
}