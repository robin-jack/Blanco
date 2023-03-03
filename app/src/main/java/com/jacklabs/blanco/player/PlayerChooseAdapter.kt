package com.jacklabs.blanco.player

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jacklabs.blanco.R
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.databinding.PlayerRowItemBinding

class PlayerChooseAdapter(private var playerList: ArrayList<Player>, private var automatic: Boolean = true) :
    RecyclerView.Adapter<PlayerChooseAdapter.ViewHolder>(), PlayerAdapter {

    override var voted: Boolean = false
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
            if (player in selectedPlayers) {
                llPlayerBox.setBackgroundResource(R.drawable.selected_player_background)
            } else {
                if (player.uid == Self.uid)
                    llPlayerBox.setBackgroundResource(R.color.white)
                else
                    llPlayerBox.setBackgroundResource(R.color.transwhite)
            }
        }

        init {
            binding.root.setOnClickListener {
                Log.v(tag, "PlayerListAdapter-setOnClickListener >>")
                val selection = playerList[layoutPosition]
                if (selection in selectedPlayers) {
                    Log.i(tag, "De-Selected ${selection.name} $layoutPosition")
                    selectedPlayers.remove(selection)
                    notifyItemChanged(layoutPosition)
                    return@setOnClickListener
                }
                else if (layoutPosition != -1 && selection.uid != Self.uid) {
                    if (selectedPlayers.size == 3) {
                        selectedPlayers.removeAt(0)
                    }
                    selectedPlayers.add(selection)
                    Log.i(tag, "Selected ${selection.name} $layoutPosition")
                    notifyDataSetChanged()
                }
            }
        }
    }

}