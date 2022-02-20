package com.nsr.pokemonapp.model.pokemon

data class PokemonItems(
    val count: Int,
    val next: String,
    val previous: String,
    val results: List<Result>
)