package com.goodrequest.hiring.viewmodel

import com.goodrequest.hiring.model.Pokemon

data class PokemonViewState(
    val pokemonList: MutableList<Pokemon>,
    val resourceState: ResourceState<Result<List<Pokemon>>>
)
