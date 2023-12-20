package com.goodrequest.hiring.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.model.Pokemon
import com.goodrequest.hiring.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokemonRepository: PokemonRepository,
) : ViewModel() {

    private val _pokemonViewState: MutableStateFlow<PokemonViewState> = MutableStateFlow(PokemonViewState(resourceState = ResourceState.Loading(), pokemonList = mutableStateListOf()))
    val pokemonViewState: StateFlow<PokemonViewState> = _pokemonViewState

    private var currentPage = 1

    init {
        loadPokemon()
    }

    /**
     * Populates the empty list of Pokemon with first page of data from the API.
     */
    private fun loadPokemon() {
        setResourceState(ResourceState.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemon(page = currentPage).collect { resourceState ->
                val pokemonList = if (resourceState is ResourceState.Success) {
                    resourceState.data.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                setViewState(pokemonList.toMutableList(), resourceState)

                if (resourceState is ResourceState.Success && pokemonList.isNotEmpty()) {
                    fetchPokemonDetails(pokemonList)
                }
            }
        }
    }

    /**
     * Loads the next page of data from the API and appends it to the existing list of Pokemon.
     */
    fun loadMorePokemon() {
        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemon(page = currentPage + 1).collect { resourceState ->
                val newPokemonList =
                    if (resourceState is ResourceState.Success) {
                        resourceState.data.getOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }

                val currentList = _pokemonViewState.value.pokemonList
                val updatedList = mutableListOf<Pokemon>().apply {
                    addAll(currentList)
                    addAll(newPokemonList)
                }

                setViewState(updatedList, resourceState)

                // Increment the page number only if the loading operation is successful
                if (resourceState is ResourceState.Success && newPokemonList.isNotEmpty()) {
                    currentPage++
                    fetchPokemonDetails(newPokemonList)
                }
            }
        }
    }

    /**
     * Refreshes the list of Pokemon with the fresh data from the API.
     */
    fun refreshPokemon() {
        setResourceState(ResourceState.Refreshing())

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemon(page = currentPage).collect { resourceState ->
                val currentList = _pokemonViewState.value.pokemonList
                val refreshedPokemonList = if (resourceState is ResourceState.Success) {
                    resourceState.data.getOrNull() ?: emptyList()
                } else {
                    currentList
                }

                setViewState(refreshedPokemonList.toMutableList(), resourceState)

                if (resourceState is ResourceState.Success && refreshedPokemonList.isNotEmpty()) {
                    fetchPokemonDetails(refreshedPokemonList)
                }
            }
        }
    }

    /**
     * Fetches the details of the Pokemon from the API.
     *
     * @param newPokemonList The list of Pokemon for which the details are to be fetched.
     */
    private fun fetchPokemonDetails(newPokemonList: List<Pokemon>) {
        viewModelScope.launch {
            val pokemonDetails = newPokemonList.map { pokemon ->
                async(Dispatchers.IO) { pokemonRepository.loadPokemonDetail(pokemon).firstOrNull() }
            }.awaitAll()

            // Update only the new Pokemon details in the existing list
            val currentList = _pokemonViewState.value.pokemonList
            newPokemonList.forEachIndexed { index, pokemon ->
                val detail = pokemonDetails[index]?.getOrNull()
                val updatedPokemon = pokemon.copy(detail = detail)

                // Find the index of the pokemon in the existing list and update it
                val pokemonIndex = currentList.indexOfFirst { it.id == pokemon.id }
                if (pokemonIndex != -1) {
                    currentList[pokemonIndex] = updatedPokemon
                } else {
                    currentList.add(updatedPokemon) // If not found, add the new Pokemon to the list
                }
            }

            // Trigger an update to notify the UI
            updatePokemonList(pokemonList = currentList)
        }
    }

    fun retry() {
        loadPokemon()
    }

    fun retryPage() {
        loadMorePokemon()
    }

    private fun setResourceState(resourceState: ResourceState<Result<List<Pokemon>>>) {
        _pokemonViewState.value = _pokemonViewState.value.copy(
            resourceState = resourceState
        )
    }

    private fun setViewState(pokemonList: MutableList<Pokemon>, resourceState: ResourceState<Result<List<Pokemon>>>) {
        _pokemonViewState.value = PokemonViewState(
            pokemonList = pokemonList,
            resourceState = resourceState
        )
    }

    private fun updatePokemonList(pokemonList: MutableList<Pokemon>) {
        _pokemonViewState.value = _pokemonViewState.value.copy(
            pokemonList = pokemonList
        )
    }
}