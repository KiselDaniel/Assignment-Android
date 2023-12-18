package com.goodrequest.hiring.viewmodel

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PokemonViewModel @Inject constructor(
    private val pokemonRepository: PokemonRepository,
) : ViewModel() {

    private val _pokemonViewState: MutableStateFlow<PokemonViewState> = MutableStateFlow(PokemonViewState(resourceState = ResourceState.Loading()))
    val pokemonViewState: StateFlow<PokemonViewState> = _pokemonViewState

    init {
        loadPokemons()
    }

    private fun loadPokemons() {
        setResourceState(ResourceState.Loading())

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemons().collect { resourceState ->
                val pokemonList =
                    if (resourceState is ResourceState.Success) {
                        resourceState.data.getOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }

                setViewState(pokemonList, resourceState)
            }

            if (_pokemonViewState.value.pokemonList.isNotEmpty()) {
                fetchPokemonDetails()
            }
        }
    }

    fun refreshPokemons() {
        var loadSuccessful = false
        setResourceState(ResourceState.Refreshing())

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemons().collect { resourceState ->
                val currentList = _pokemonViewState.value.pokemonList
                val pokemonList = if (resourceState is ResourceState.Success) {
                    loadSuccessful = true
                    resourceState.data.getOrNull() ?: emptyList()
                } else {
                    if (currentList.isNotEmpty()) currentList else emptyList()
                }

                setViewState(pokemonList, resourceState)
            }

            if (loadSuccessful && _pokemonViewState.value.pokemonList.isNotEmpty()) {
                fetchPokemonDetails()
            }
        }
    }

    private fun fetchPokemonDetails() {
        viewModelScope.launch {
            val currentList = _pokemonViewState.value.pokemonList
            val pokemonDetails = currentList.map { pokemon ->
                async(Dispatchers.IO) { pokemonRepository.loadPokemonDetail(pokemon).first() }
            }.awaitAll()

            _pokemonViewState.value = _pokemonViewState.value.copy(
                pokemonList = currentList.mapIndexed { index, pokemon ->
                    pokemon.copy(detail = pokemonDetails[index].getOrNull())
                }
            )
        }
    }

    fun retry() {
        loadPokemons()
    }

    private fun setResourceState(resourceState: ResourceState<Result<List<Pokemon>>>) {
        _pokemonViewState.value = _pokemonViewState.value.copy(
            resourceState = resourceState
        )
    }

    private fun setViewState(pokemonList: List<Pokemon>, resourceState: ResourceState<Result<List<Pokemon>>>) {
        _pokemonViewState.value = PokemonViewState(
            pokemonList = pokemonList,
            resourceState = resourceState
        )
    }
}