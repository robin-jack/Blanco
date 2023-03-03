package com.jacklabs.blanco.room

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.player.Player
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Room(
    val host: String? = null,
    val hostUid: String? = null,
    val players: HashMap<String, Player> = hashMapOf(),
    val lastOut: String = "No one",
    val mode: String = "classic",
    val round: Int = 1,
    val state: String = RoomState.open,
    val selectedBlancos: ArrayList<String> = arrayListOf(),
    val word: String? = null,
    val voteDuration: Int = 10,
    val voteTime: Long = 0,
    val votes: HashMap<String, String> = hashMapOf(),
    @DocumentId
    val id: String? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    fun self(): Player {
        return players[Self.uid]!!
    }
    fun playersList(): ArrayList<Player> {
        val sortedList = players.values.sortedWith(compareBy({ it.votedOut }, { it.order }))
        return sortedList.toCollection(ArrayList())
    }
}
