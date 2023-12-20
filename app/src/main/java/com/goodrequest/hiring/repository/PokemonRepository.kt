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
    /**
     * Fetches a list of Pokemon from the API.
     *
     * @param page The page number to fetch from the API.
     * @return A [Flow] that emits [ResourceState] objects representing the result of the fetch operation.
     * The [ResourceState] can be one of the following:
     * - [ResourceState.Loading]: Indicates that the fetch operation is in progress.
     * - [ResourceState.Success]: Indicates that the fetch operation was successful. Contains the list of Pokemon fetched from the API.
     * - [ResourceState.Error]: Indicates that the fetch operation failed.
     *
     * @throws Exception If an error occurs while fetching the data.
     */
    suspend fun loadPokemon(page: Int): Flow<ResourceState<Result<List<Pokemon>>>> {
        return flow {
            // just to simulate network delay for displaying loading state
            delay(500)
            try {
                val result = api.getPokemons(page = page)
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

    /**
     * Fetches the detail of a specific Pokemon from the API.
     *
     * @param pokemon The Pokemon object for which the detail is to be fetched.
     * @return A [Flow] that emits [Result] objects representing the result of the fetch operation.
     * The [Result] can be one of the following:
     * - [Result.success]: Indicates that the fetch operation was successful. Contains the PokemonDetail fetched from the API.
     * - [Result.failure]: Indicates that the fetch operation failed.
     *
     * @throws Exception If an error occurs while fetching the data.
     */
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