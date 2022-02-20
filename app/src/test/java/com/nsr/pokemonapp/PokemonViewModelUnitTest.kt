package com.nsr.pokemonapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nsr.pokemonapp.model.pokemon.PokemonItems
import com.nsr.pokemonapp.model.pokemoninfo.PokemonInfo
import com.nsr.pokemonapp.network.APIHelper
import com.nsr.pokemonapp.network.ResponseResource
import com.nsr.pokemonapp.network.RetrofitBuilder
import com.nsr.pokemonapp.network.RetrofitBuilder.INITIAL_URL
import com.nsr.pokemonapp.network.Status
import com.nsr.pokemonapp.viewmodels.PokemonItem
import com.nsr.pokemonapp.viewmodels.PokemonItemsList
import com.nsr.pokemonapp.viewmodels.PokemonViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PokemonViewModelUnitTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    @Mock
    private lateinit var apiHelper: APIHelper

    @Mock
    private lateinit var apiUsersObserver: Observer<ResponseResource<PokemonItemsList>>

    @Test
    fun `test pokemon API for Initial loading status`() {
        testCoroutineRule.runBlockingTest {
            doReturn(PokemonItems(count = 3, next = "", previous = "", results = emptyList()))
                .`when`(apiHelper)
                .getPokemonList(INITIAL_URL)
            val viewModel = PokemonViewModel(apiHelper)
            viewModel.pokemonInfo.observeForever(apiUsersObserver)
            verify(apiHelper).getPokemonList(INITIAL_URL)
            verify(apiUsersObserver).onChanged(ResponseResource.loading())
            viewModel.pokemonInfo.removeObserver(apiUsersObserver)
        }
    }

    @Test
    fun `test pokemon API for error status`() {
        val observerSequence = mockk<Observer<ResponseResource<PokemonItemsList>>> { every { onChanged(any()) } just Runs }

        testCoroutineRule.runBlockingTest {
            doReturn(Exception())
                .`when`(apiHelper)
                .getPokemonList(INITIAL_URL)
            val viewModel = PokemonViewModel(apiHelper)
            viewModel.pokemonInfo.observeForever(observerSequence)
            verify(apiHelper).getPokemonList(INITIAL_URL)

            verifySequence {
                observerSequence.onChanged(ResponseResource.loading())
                observerSequence.onChanged(ResponseResource.error(data = PokemonItemsList(), message = R.string.error_msg))
            }
        }

    }
}