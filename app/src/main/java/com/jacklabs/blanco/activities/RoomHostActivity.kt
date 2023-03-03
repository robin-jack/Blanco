package com.jacklabs.blanco.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.jacklabs.blanco.R
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.fragments.RoomSettingsFragment
import com.jacklabs.blanco.fragments.WordInputFragment
import com.jacklabs.blanco.player.PlayerChooseAdapter
import com.jacklabs.blanco.player.PlayerListAdapter
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomSettings
import com.jacklabs.blanco.viewmodels.RoomHostViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class RoomHostActivity : RoomActivity(), RoomSettingsFragment.GameSettings {

    override val model: RoomHostViewModel by viewModels()
    private val tag = "RoomHostActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btHostStart.visibility = View.VISIBLE
        binding.btPopulate.visibility = View.VISIBLE

        lifecycleScope.launch {
            model.word.collect {
                Log.d(tag, "Word received")
                startGame(it)
            }
        }

        //see if users online or not
        Handler(Looper.getMainLooper()).postDelayed({
            model.usersListener()
        }, 3000)

        Self.listeners.forEach {
            Log.e(tag, it.toString())
        }

        binding.llRoomBox.setOnClickListener {
            val fragment = RoomSettingsFragment.newInstance(model.roomSettings)
            fragment.show(supportFragmentManager, "Room settings")
        }

        binding.btStartVote.setOnClickListener {
            model.startVote()
        }

        binding.btHostStart.setOnClickListener {
            playersList = model.roomRaw.playersList()
            if (playersList.size < minimumPlayers) {
                Toast.makeText(this,
                    "Need ${minimumPlayers-playersList.size} more player(s)!", Toast.LENGTH_SHORT).show()
            } else {
                if (!model.roomSettings.automatic && adapter.selectedPlayers.size < 3) {
                    Toast.makeText(this,
                        "Select ${3 - adapter.selectedPlayers.size} Blanco player(s)!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val dialog = WordInputFragment()
                dialog.show(supportFragmentManager, "WordInput")
            }
        }

        binding.btHostPlayAgain.setOnClickListener {
            model.resetRoom(model.roomRaw)
        }

        binding.btPopulate.setOnClickListener {
            model.populate()
        }
    }

    override fun attachAdapter(room: Room){
        Log.d(tag, "Attaching Adapter")
        playersList = room.playersList()
        adapter = PlayerChooseAdapter(playersList, model.roomSettings.automatic)
        binding.rvPlayersList.adapter = adapter as PlayerChooseAdapter
        (adapter as PlayerChooseAdapter).notifyDataSetChanged()
        roomCollector()
    }

    private fun startGame(word: String) {
        Log.d(tag, "######## < Game started! > ########")
        Log.i(tag, "Closing users listener")
        model.usersRef.removeEventListener(model.usersListener)
        model.setFields(word, model.roomRaw.players)
        hideHostControls()
    }

    private fun hideHostControls() {
        binding.btHostStart.visibility = View.GONE
        binding.btPopulate.visibility = View.GONE
        binding.btStartVote.visibility = View.VISIBLE
        binding.btVote.visibility = View.VISIBLE
    }

    override fun applySettings(roomSettings: RoomSettings) {
        model.roomSettings = roomSettings
        Log.d(tag, "SETTINGS: $roomSettings")
    }

}