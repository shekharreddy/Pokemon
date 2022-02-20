package com.nsr.pokemonapp.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nsr.pokemonapp.databinding.FragmentPokemonBinding
import com.nsr.pokemonapp.network.APIHelper
import com.nsr.pokemonapp.network.RetrofitBuilder
import com.nsr.pokemonapp.R
import com.nsr.pokemonapp.network.ResponseResource
import com.nsr.pokemonapp.network.Status
import com.nsr.pokemonapp.viewmodels.*

/**
 * Created by Shekhar Reddy
 * A fragment representing a list of Pokemon Items.
 */
class PokemonFragment : Fragment() {
    private val TAG = "PokemonFragment"
    private var _binding: FragmentPokemonBinding? = null
    private lateinit var pokemonItemAdapter: PokemonAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: PokemonViewModel by activityViewModels {
        ViewModelFactory(APIHelper(RetrofitBuilder.apiService))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPokemonBinding.inflate(inflater, container, false)
        pokemonItemAdapter = PokemonAdapter { pokemon -> adapterOnClick(pokemon) }
        // Set the adapter
        binding.recyclerView.apply {
            adapter = pokemonItemAdapter
            addItemDecoration(
                RecyclerViewItemDecoration(
                    spacing = resources.getDimensionPixelSize(R.dimen.recycler_view_item_spacing)
                )
            )
        }
        return binding.root
    }

    /* Opens PokemonDetailFragment when RecyclerView item is clicked. */
    private fun adapterOnClick(pokemonItem: PokemonBaseItem) {
        when (pokemonItem) {
            is PokemonItem -> {
                viewModel.updateSelectedPokemon(pokemonItem)
                Log.i(TAG, "Item Cliked ${pokemonItem}")
                findNavController().navigate(R.id.action_to_details_screen)
            }
            is LoadMoreItems -> {
                viewModel.getNextPageURL()?.let {
                    viewModel.fetchPokemonList(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun showErrorMessage(msgRes: Int) {
        Toast.makeText(context, resources.getString(msgRes), Toast.LENGTH_LONG).show()
    }

    private fun setupObservers() {
        viewModel.pokemonInfo.observe(viewLifecycleOwner) {
            it?.let { response ->
                handleResponse(response)
            }?: run {
                showErrorMessage(R.string.error_msg)
            }
        }
    }
    private fun handleResponse(response: ResponseResource<PokemonItemsList>) {
        with(binding) {
            when (response.status) {
                Status.SUCCESS -> {
                    recyclerView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    response.data?.let { itemsData ->
                        pokemonItemAdapter.submitList(itemsData.allDisplayItems)
                    }
                }
                Status.ERROR -> {
                    recyclerView.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    showErrorMessage(response.message)
                }
                Status.INITIAL_LOADING -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                Status.MORE_ITEMS_LOADING -> {
                    response.data?.let { itemsData ->
                        pokemonItemAdapter.submitList(itemsData.allDisplayItems)
                    }
                }
            }
        }
    }
}



