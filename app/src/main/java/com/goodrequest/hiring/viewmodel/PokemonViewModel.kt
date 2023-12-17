package com.goodrequest.hiring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.repository.PokemonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        _pokemonViewState.value = _pokemonViewState.value.copy(
            resourceState = ResourceState.Loading()
        )

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemons().collect { resourceState ->
                val pokemonList =
                    if (resourceState is ResourceState.Success) {
                        resourceState.data.getOrNull() ?: emptyList()
                    } else {
                        emptyList()
                    }

                _pokemonViewState.value = PokemonViewState(
                    pokemonList = pokemonList,
                    resourceState = resourceState
                )
            }
        }
    }

    fun refreshPokemons() {
        _pokemonViewState.value = _pokemonViewState.value.copy(
            resourceState = ResourceState.Refreshing()
        )

        viewModelScope.launch(Dispatchers.IO) {
            pokemonRepository.loadPokemons().collect { resourceState ->
                val currentList = _pokemonViewState.value.pokemonList
                val pokemonList = if (resourceState is ResourceState.Success) {
                    resourceState.data.getOrNull() ?: emptyList()
                } else {
                    if (currentList.isNotEmpty()) currentList else emptyList()
                }

                _pokemonViewState.value = PokemonViewState(
                    pokemonList = pokemonList,
                    resourceState = resourceState
                )
            }
        }
    }


    fun retry() {
        loadPokemons()
    }
}