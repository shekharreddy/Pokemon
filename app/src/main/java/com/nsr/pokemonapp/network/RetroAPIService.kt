package com.nsr.pokemonapp.network


import com.nsr.pokemonapp.model.pokemon.PokemonItems
import com.nsr.pokemonapp.model.pokemoninfo.PokemonInfo
import retrofit2.http.GET
import retrofit2.http.Url

interface RetroAPIService {
    @GET
    suspend fun getPokemonList(@Url url:String): PokemonItems

    @GET
    suspend fun getPokemonDetails(@Url url: String): PokemonInfo
}