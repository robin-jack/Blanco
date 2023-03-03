package com.jacklabs.blanco

import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.snapshots
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.jacklabs.blanco.Self.roomId
import com.jacklabs.blanco.Self.uid
import com.jacklabs.blanco.player.Player
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


object Self {
    var uid: String? = null
    var name: String? = null
    var host: Boolean = false
    var roomId: String? = null
    var pnvRef: String? = null
    val listeners = mutableListOf<ListenerRegistration>()

    private const val tag = "Self"

    fun playerObject(): Player {
        return Player(
            uid=uid,
            name=name,
            host=host
        )
    }

    fun resetSelf() {
        name = null
        host = false
        roomId = null
        pnvRef = null
        Log.d(tag, "Self was reset")
    }

    fun setVoteTime() {
        val rtdb = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/")
        val dbRef = rtdb.getReference("rooms/$roomId/voteTime")
        val voteTime = System.currentTimeMillis() + (30 * 1000L)
        dbRef.setValue(voteTime)
    }

    suspend fun getVoteTime() {
        val rtdb = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/")
        Log.d(tag, "Getting voteTime")
        val dbRef = rtdb.getReference("rooms/$roomId/voteTime")
        dbRef.get().addOnSuccessListener {vote ->
            Log.d(tag, "offset: $offset")
            Log.d(tag, "voteTime: ${vote.value}")
            val current = System.currentTimeMillis()
            Log.d(tag, "Current: $current")
            val tt = (vote.value as Long - (offset)) - current
            Log.d(tag, "chomp: $tt")
        }

    }

    fun test() {
        Log.d(tag, "Test")
        val rtdb = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/")
        val dbRef = rtdb.getReference("rooms/$roomId/time")
        // Get the current client time
        val clientTime = System.currentTimeMillis()
        // Set and get the current server time
        dbRef.setValue(ServerValue.TIMESTAMP)
        val serverTime = dbRef.get().addOnSuccessListener {
            val sv = it.value as Long
            val tf = System.currentTimeMillis() - clientTime
            Log.d(tag, "MIAUMIAU: $tf")
        }

    }

    var offset: Long = 0
    fun setAsOnline() {
        Log.d(tag, "Start of set online")
        val rtdb = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/")
        val offsetRef = Firebase.database.getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                offset = snapshot.value as Long
                Log.d(tag, offset.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Listener was cancelled")
            }
        })
        Log.d(tag, "Set as Online")
        val dbRef = rtdb.getReference("rooms/$roomId/players/$uid")
        dbRef.setValue(true)
        dbRef.onDisconnect()
            .setValue(false)
    }

    fun setAsOffline() {
        val rtdb = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/")
        val dbRef = rtdb.getReference("rooms/$roomId/players/$uid")
        dbRef.setValue(false)
    }
}