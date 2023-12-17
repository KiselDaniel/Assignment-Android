package com.goodrequest.hiring.model

data class Pokemon(
    val id     : String,
    val name   : String,
    val detail : PokemonDetail? = null)
