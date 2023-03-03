package com.jacklabs.blanco.player

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jacklabs.blanco.databinding.PlayerRowItemBinding

interface PlayerAdapter {

    abstract var voted: Boolean
    abstract val selectedPlayer: Player?
    abstract var selectedPlayers: ArrayList<Player>

    fun setPlayers(playersList: ArrayList<Player>) {

    }

}