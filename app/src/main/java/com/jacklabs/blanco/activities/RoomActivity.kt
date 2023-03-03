package com.jacklabs.blanco.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jacklabs.blanco.player.PlayerListAdapter
import com.jacklabs.blanco.databinding.ActivityRoomBinding
import com.jacklabs.blanco.viewmodels.RoomViewModel
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.fragments.ExitRoomFragment
import com.jacklabs.blanco.fragments.ShowWordFragment
import com.jacklabs.blanco.player.Player
import com.jacklabs.blanco.player.PlayerAdapter
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round


@ExperimentalCoroutinesApi
open class RoomActivity : AppCompatActivity() {

    protected open val model: RoomViewModel by viewModels()
    protected lateinit var binding: ActivityRoomBinding
    lateinit var adapter: PlayerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    protected val minimumPlayers: Int = 5
    private lateinit var dialog: ShowWordFragment
    protected lateinit var playersList: ArrayList<Player>

    private var needsReset = false

    private val tag = "RoomActivity"

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        linearLayoutManager = LinearLayoutManager(this)
        binding.rvPlayersList.layoutManager = linearLayoutManager

        Self.setAsOnline()

        if (intent.getBooleanExtra("recon", false)) reconnect()

        lifecycleScope.launch {
            attachAdapter(
                model.listenToRoom()
                    .flowOn(Dispatchers.IO)
                    .first()
            )
        }

        /*binding.llRoomBox.setOnClickListener {
            val dialog = RoomSettingsFragment()
            dialog.show(supportFragmentManager, "Room settings")
        }
        */

        binding.btVote.setOnClickListener {
            if (adapter.selectedPlayer != null) {
                Log.d(tag, "Player ${adapter.selectedPlayer!!.name}")
                Toast.makeText(this, "Voted ${adapter.selectedPlayer!!.name}", Toast.LENGTH_LONG).show()
                model.castVote(adapter.selectedPlayer!!.uid, adapter.selectedPlayer!!.name!!)
                binding.btVote.isEnabled = false
                adapter.voted = true
            } else {
                Toast.makeText(this, "Select a player to vote!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun reconnect() {
        Log.d(tag, "Reconnected!")
        binding.btVote.visibility = View.VISIBLE
        binding.btVote.isEnabled = false
        if (Self.host)
            binding.btStartVote.isEnabled = true
    }

    open fun attachAdapter(room: Room) {
        Log.d(tag, "Attaching Adapter")
        playersList = room.playersList()
        adapter = PlayerListAdapter(playersList)
        binding.rvPlayersList.adapter = adapter as? PlayerListAdapter
        (adapter as PlayerListAdapter).notifyDataSetChanged()
        roomCollector()
    }


    private fun updateRoom(room: Room) {
        when (room.state) {
            RoomState.open -> {
                if (needsReset) resetRoom()
                adapter.setPlayers(room.playersList())
                if (playersList.size >= minimumPlayers)
                    binding.etRoomText.text = "Waiting for host..."
            }
            RoomState.firstRound -> {
                adapter.setPlayers(room.playersList())
                Log.d(tag, "Started first round: Round ${room.round}")
                Log.d(tag, "Setting vote duration to ${room.voteDuration}s")
                model.voteDuration = room.voteDuration * 1000L
                dialog = if (room.self().blanco) {
                    ShowWordFragment.newInstance("Blanco")
                } else {
                    ShowWordFragment.newInstance(room.word!!)
                }
                dialog.show(supportFragmentManager, "ShowWord")
                binding.etRoomText.text = "ROUND ${room.round}"
                binding.btVote.visibility = View.VISIBLE
                binding.btVote.isEnabled = false
                if (Self.host)
                    binding.btStartVote.isEnabled = true
            }
            RoomState.playing -> {
                adapter.setPlayers(room.playersList())
                Log.d(tag, "Started Round ${room.round}")
                Toast.makeText(this, "${room.lastOut} was voted out", Toast.LENGTH_LONG).show()
                binding.etRoomText.text = "ROUND ${room.round}"
                if (Self.host)
                    binding.btStartVote.isEnabled = true
            }
            RoomState.voting -> {
                Log.d(tag, "Vote started")
                Log.i(tag, "Removing room listener")
                model.listener.remove()
                binding.etRoomText.text = "VOTE!"
                if (Self.host)
                    binding.btStartVote.isEnabled = false
                binding.btVote.isEnabled = !room.self().votedOut
                adapter.voted = false
                val timeLeft = round(((room.voteTime - Self.offset) - System.currentTimeMillis()) / 1000.0).toInt()
                voteCountdown(timeLeft)
            }
            RoomState.ended -> {
                adapter.setPlayers(room.playersList())
                Log.d(tag, "Started Round ${room.round}")
                Toast.makeText(this, "${room.lastOut} was voted out", Toast.LENGTH_LONG).show()
                binding.etRoomText.text = "GAME ENDED!"
                binding.btVote.visibility = View.GONE
                binding.btStartVote.visibility = View.GONE
                if (Self.host)
                    binding.btHostPlayAgain.visibility = View.VISIBLE
                needsReset = true

            }
            else -> Unit
        }
    }

    protected fun roomCollector() {
        lifecycleScope.launch {
            Log.i(tag, " # Opening room collector # ")
            model.listenToRoom()
                .flowOn(Dispatchers.IO)
                .collect { room ->
                    Log.i(tag, "<<Receiving data")
                    updateRoom(room)
            }
        }
    }

    private fun voteCountdown(timeLeft: Int) {
        val job = Job()
        lifecycleScope.launch {
            Log.d(tag, "timeLeft: $timeLeft")
            model.startVoteCountdown(timeLeft, job).collectLatest {
                binding.etRoomText.text = it
            }
        }
        lifecycleScope.launch {
            model.listenToRoom().first{ it.state != "voting" }.let {
                job.cancel()
                Log.d(tag, "Vote ended")
                Log.d(tag, " # Re-Opening Collector # ")
                binding.etRoomText.text = "VOTE ENDED"
                binding.btVote.isEnabled = false
                delay(2000)
                roomCollector()
            }
        }
    }

    private fun resetRoom() {
        Log.d(tag, "Room reset")
        recreate()
    }

    override fun onBackPressed() {
        val dialog = ExitRoomFragment.newInstance(model.roomRaw.state)
        dialog.show(supportFragmentManager, "ExitRoom")
    }

    override fun onStop() {
        super.onStop()
        Self.setAsOffline()
    }

}