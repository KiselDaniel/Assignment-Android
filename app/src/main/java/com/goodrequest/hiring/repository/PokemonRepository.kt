package com.goodrequest.hiring.repository

import com.goodrequest.hiring.model.Pokemon
import com.goodrequest.hiring.model.PokemonDetail
import com.goodrequest.hiring.network.PokemonApi
import com.goodrequest.hiring.viewmodel.ResourceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class PokemonRepository(
    private val api: PokemonApi
) {
    suspend fun loadPokemons(): Flow<ResourceState<Result<List<Pokemon>>>> {
        return flow {
            // just to simulate network delay for displaying loading state
            delay(500)
            try {
                val result = api.getPokemons(page = 1)
                if (result.isSuccess) {
                    emit(ResourceState.Success(result))
                }
                else if (result.isFailure) {
                    emit(ResourceState.Error("Fetching the data failed!"))
                }
            } catch (e: Exception) {
                emit(ResourceState.Error(e.message ?: "Error fetching the data!"))
            }
        }
    }

    suspend fun loadPokemonDetail(pokemon: Pokemon): Flow<Result<PokemonDetail>> {
        return flow {
                val result = api.getPokemonDetail(pokemon)
                if (result.isSuccess) {
                    emit(result)
                }
        }.catch { e ->
            emit(Result.failure(Exception("Error fetching the pokemon detail!", e)))
        }
    }
}