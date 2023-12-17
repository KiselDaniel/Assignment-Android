package com.goodrequest.hiring.viewmodel

import com.goodrequest.hiring.model.Pokemon

data class PokemonViewState(
    val pokemonList: List<Pokemon> = emptyList(),
    val resourceState: ResourceState<Result<List<Pokemon>>>
)
