package com.nsr.pokemonapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nsr.pokemonapp.R
import com.nsr.pokemonapp.model.pokemon.PokemonItems
import com.nsr.pokemonapp.model.pokemoninfo.PokemonInfo
import com.nsr.pokemonapp.model.pokemoninfo.Stat
import com.nsr.pokemonapp.network.APIHelper
import com.nsr.pokemonapp.network.ResponseResource
import com.nsr.pokemonapp.network.RetrofitBuilder.INITIAL_URL
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll


/***
 * Created by Shekhar Reddy
 * ViewModel to fetch Pokemon list and details
 */
class PokemonViewModel(private val apiHelper: APIHelper) : ViewModel() {

    //Data model to hold list of Main API response
    private lateinit var pokemonItems: PokemonItems

    //Data model to hold all the data required for UI to show and nextPageURL
    private var pokemonData: PokemonItemsList = PokemonItemsList()

    private val mutableSelectedPokemonItem = MutableLiveData<PokemonItem>()
    val selectedPokemonItem: LiveData<PokemonItem> get() = mutableSelectedPokemonItem

    private val requiredStatsList = listOf("speed", "attack", "defense")

    private val _pokemonInfo: MutableLiveData<ResponseResource<PokemonItemsList>> by lazy {
        MutableLiveData<ResponseResource<PokemonItemsList>>()
    }

    val pokemonInfo : LiveData<ResponseResource<PokemonItemsList>>
        get() = _pokemonInfo

    //Set selected item which will be observed in Details screen
    fun updateSelectedPokemon(selectedItem: PokemonItem){
        mutableSelectedPokemonItem.value = selectedItem
    }

    //Get next page URL to load data
    fun getNextPageURL() = pokemonData.nextPageURL

    init {
        fetchPokemonList()
    }

    // Filter required stats and build a string
    private fun getStatsDetails(stats: List<Stat>? = null) : List<StatsDetails>?  =
        stats?.filter {
            it.stat.name in requiredStatsList
        }?.map {
            StatsDetails(it.stat.name.replaceFirstChar { type -> type.uppercase() }, it.base_stat, it.effort)
        }

    //To update loading status on the UI
    private fun updateLoadingStatus(){
        _pokemonInfo.value =
            if(pokemonData.pokemonItems.isEmpty())
                ResponseResource.loading()
            else
                ResponseResource.loadingMoreItems(data = pokemonData.apply {
                    loadMoreItem = loadMoreItem.copy(loadingStatus = R.string.loading_more_items, isEnabled = false)
                })
    }

    //Create list of pokemon with required fields to show On UI from API response
    private fun getAllPokemonDetails( responses: List<Pair<String, PokemonInfo>>? = null): List<PokemonItem>  {
        val pokemonItems = mutableListOf<PokemonItem>()
        responses?.forEach { (_, response) ->
            pokemonItems.run {
                add(
                    PokemonItem(
                        response.name.replaceFirstChar { it.uppercase() },
                        response.sprites.getImageURL(),
                        response.types.joinToString { it.type.name.replaceFirstChar { type -> type.uppercase() } },
                        response.abilities.joinToString { it.ability.name.replaceFirstChar { type -> type.uppercase() } },
                        getStatsDetails(response.stats)
                    )
                )
            }

        }
        return pokemonItems
    }

    //Fetch list of Pokemon and details using service
    // Currently offset and limit set to 10
    fun fetchPokemonList(url: String = INITIAL_URL) {
        updateLoadingStatus()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    pokemonItems = apiHelper.getPokemonList(url)
                    val multipleURLs: List<String> = pokemonItems.results.map { it.url }
                    val runningTasks = multipleURLs.map { url ->
                        async { // this will allow us to run multiple tasks in parallel
                            val apiResponse = apiHelper.getPokemonDetails(url)
                            url to apiResponse // associate id and response for later
                        }
                    }
                    val responses = runningTasks.awaitAll()
                    updateLiveData(getAllPokemonDetails(responses), pokemonItems.next)
                } catch (e: Exception) {
                    _pokemonInfo.postValue(ResponseResource.error(message = R.string.error_msg, data = pokemonData))
                }
            }
        }
    }

    //Update PokemonItemsList with latest data and live data
    private fun updateLiveData(listOfPokemon: List<PokemonItem>, nextURL: String?= null){
        pokemonData.apply {
            pokemonItems = pokemonItems.plus(listOfPokemon)
            loadMoreItem = loadMoreItem.apply {
                loadingStatus = R.string.load_more_items
                isEnabled = true
            }
            nextPageURL = nextURL
        }
        _pokemonInfo.postValue(ResponseResource.success(data = pokemonData))
    }
}

//To construct all the required data to UI
data class PokemonItemsList(var pokemonItems: List<PokemonBaseItem> = emptyList(),
                            var loadMoreItem: LoadMoreItems = LoadMoreItems(),
                            var nextPageURL: String?= null) {
    val allDisplayItems: List<PokemonBaseItem>
        get() = pokemonItems + if(nextPageURL.isNullOrEmpty()) emptyList() else listOf(loadMoreItem)
}

sealed class PokemonBaseItem

data class PokemonItem(val name: String,
                       val imageURL: String? = "",
                       val types: String,
                       val abilities: String,
                       val stats: List<StatsDetails>? = emptyList()) : PokemonBaseItem()

data class StatsDetails(val name: String,
                        val baseStat: Int,
                        val effort: Int)

data class LoadMoreItems(var loadingStatus: Int = R.string.load_more_items,
                         var isEnabled: Boolean = false) : PokemonBaseItem()