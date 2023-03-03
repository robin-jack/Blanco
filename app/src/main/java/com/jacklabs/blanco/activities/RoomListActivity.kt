package com.jacklabs.blanco.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.Self.roomId
import com.jacklabs.blanco.Self.setAsOnline
import com.jacklabs.blanco.Self.uid
import com.jacklabs.blanco.databinding.ActivityRoomListBinding
import com.jacklabs.blanco.player.Player
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomListAdapter
import com.jacklabs.blanco.room.RoomState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import java.lang.Exception

@ExperimentalCoroutinesApi
class RoomListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityRoomListBinding
    private lateinit var adapter: RoomListAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val tag = "RoomListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoomListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        linearLayoutManager = LinearLayoutManager(this)
        binding.rvRoomsList.layoutManager = linearLayoutManager

        getRooms()

        binding.btRefresh.setOnClickListener {
            getRooms()
            Log.i(tag, "Refreshed!")
        }

        binding.btCreate.setOnClickListener {
            createFake()
        }
    }

    private fun getRooms() {
        lifecycleScope.launchWhenStarted {
            try {
                db = FirebaseFirestore.getInstance()
                val roomsRef = db.collection("games").whereNotEqualTo("state", "ended").get().await()
                val rooms = roomsRef.toObjects(Room::class.java) as ArrayList<Room>
                updateRoomsList(rooms)
            } catch (e: Exception){
                Log.w(tag, e)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRoomsList(rooms: ArrayList<Room>) {
        adapter = RoomListAdapter(rooms)
        binding.rvRoomsList.adapter = adapter
        adapter.setOnItemClickListener(object: RoomListAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val room = rooms[position]
                if (room.state == RoomState.open) {
                    joinRoom(room)
                } else {
                    Toast.makeText(this@RoomListActivity, "Cant join room, game already started!", Toast.LENGTH_LONG).show()
                }
            }
        })
        adapter.notifyDataSetChanged()
    }

    // TODO: make model view

    private fun joinRoom(room: Room) {
        val roomRef = db.collection("games").document(room.id!!)
        roomRef.get()
            .addOnSuccessListener { doc ->
                roomId = doc.id
                val intent = Intent(this@RoomListActivity, RoomActivity::class.java)
                if (uid !in room.players) {
                    roomRef.update("players.$uid", Self.playerObject())
                } else {
                    intent.putExtra("recon", true)
                }
                startActivity(intent) // TO ROOM
                Log.d(tag, "Joined game!")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error joining game", e)
            }
    }

    private fun createFake() {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        val roomRef: DocumentReference = db.collection("games").document(Self.roomId!!)
        val generatedPlayers: HashMap<String, Player> = hashMapOf(
            uid!! to Self.playerObject(),
            "jYhsBvZwpIkLH" to Player("jYhsBvZwpIkLH", "luke"),
            "aBvCfDgFdFwGf" to Player("aBvCfDgFdFwGf", "mike"),
            "GfTnoItgDNoId" to Player("GfTnoItgDNoId", "john"),
            "MXlFvPwjGvUoW" to Player("MXlFvPwjGvUoW", "mary"),
            "qPsOFlgNzHfRt" to Player("qPsOFlgNzHfRt", "zoe")
        )

        roomRef.update("players", generatedPlayers)
        Log.i(tag, "Generated players")
    }
}