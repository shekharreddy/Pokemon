package com.nsr.pokemonapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nsr.pokemonapp.R

/***
 * Created by Shekhar Reddy
 * Activity to show Pokemon list
 */
class PokemonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}