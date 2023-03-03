package com.jacklabs.blanco.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jacklabs.blanco.R
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.databinding.PlayerRowItemBinding

class PlayerListAdapter(private var playerList: ArrayList<Player>) :
    RecyclerView.Adapter<PlayerListAdapter.ViewHolder>(), PlayerAdapter {

    override var voted: Boolean =  true
    override var selectedPlayer: Player? = null
    override var selectedPlayers: ArrayList<Player> = arrayListOf()
    private val tag = "PlayerListAdapter"

    override fun setPlayers(playersList: ArrayList<Player>) {
        Log.d(tag, "Setting players")
        playerList.clear()
        playerList.addAll(playersList)
        selectedPlayer = null
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =  PlayerRowItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(playerList[position])
    }

    override fun getItemCount() = playerList.size

    @SuppressLint("NotifyDataSetChanged")
    inner class ViewHolder(private val binding: PlayerRowItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: Player) = with(binding) {
            tvPlayerName.text = player.name
            vwPlayerColor.setBackgroundColor(player.color)
            //if == -1
            if (playerList[layoutPosition] == selectedPlayer) {
                llPlayerBox.setBackgroundResource(R.drawable.selected_player_background)
            } else {
                if (player.uid == Self.uid)
                    llPlayerBox.setBackgroundResource(R.color.white)
                else
                    llPlayerBox.setBackgroundResource(R.color.transwhite)
            }
            if (player.votedOut) {
                llPlayerBox.setBackgroundResource(R.color.transgray)
            }
        }

        init {
            binding.root.setOnClickListener {
                Log.v(tag, "PlayerListAdapter-setOnClickListener >>")
                val selection = playerList[layoutPosition]
                if (!voted && selection == selectedPlayer) {
                    Log.i(tag, "De-Selected ${selectedPlayer!!.name} $layoutPosition")
                    //selectedItemPosition = -1
                    selectedPlayer = null
                    notifyItemChanged(layoutPosition)
                    return@setOnClickListener
                }
                if (layoutPosition != -1 && !voted && !selection.votedOut && !selection.host && selection.uid != Self.uid) {
                    //selectedItemPosition = layoutPosition
                    selectedPlayer = selection
                    Log.i(tag, "Selected ${selectedPlayer!!.name} $layoutPosition")
                    notifyDataSetChanged()
                }
            }
        }
    }

}