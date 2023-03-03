package com.jacklabs.blanco.viewmodels

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat.postDelayed
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.activities.RoomHostActivity
import com.jacklabs.blanco.player.Player
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomSettings
import com.jacklabs.blanco.room.RoomState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@ExperimentalCoroutinesApi
class RoomHostViewModel: RoomViewModel() {

    internal lateinit var usersRef: DatabaseReference
    internal lateinit var usersListener: ValueEventListener

    private var _word = MutableSharedFlow<String>()
    internal val word = _word.asSharedFlow()

    var roomSettings = RoomSettings()

    private var extra = false

    private val tag = "RoomHostViewModel"

    @Suppress("UNCHECKED_CAST")
    fun usersListener() {
        Log.i(tag, "Opening users listener")
        usersRef = FirebaseDatabase.getInstance("https://blanco-pro-default-rtdb.europe-west1.firebasedatabase.app/").getReference("/rooms/${Self.roomId}/players")
        val handlerMap = HashMap<String, Handler>()
        usersListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val users: HashMap<String, Boolean> = dataSnapshot.value as HashMap<String, Boolean>
                for ((user, online) in users) {
                    if (!online) {
                        // offline user, schedule removal in x seconds
                        val handler = Handler(Looper.getMainLooper())
                        handlerMap[user] = handler
                        handler.postDelayed({
                            val remove = mapOf("players.${user}" to FieldValue.delete())
                            roomRef.update(remove)
                            handlerMap.remove(user)
                            Log.d(tag, "removed: $user")
                        }, 5000)
                    } else {
                        if (user in handlerMap.keys) {
                            // back online, cancel removal
                            val handly = handlerMap[user]
                            handly?.removeCallbacksAndMessages(null)
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(tag, "loadUsers:onCancelled", databaseError.toException())
            }
        }
        usersRef.addValueEventListener(usersListener)
    }

    fun startVote() {
        val vt = (System.currentTimeMillis() + Self.offset) + voteDuration
        db.runBatch { batch ->
            batch.update(roomRef, "state", RoomState.voting)
            batch.update(roomRef, "voteTime", vt)
        }.addOnCompleteListener {
            Log.d(tag, "Success on write to start vote")
            getVotes()
        }.addOnFailureListener {
            Log.d(tag, "Error on write to start vote ${it.message}")
        }
    }

    private fun getVotes() {
        viewModelScope.launch {
            Log.d(tag, ">>Getting votes ·")
            val result = withTimeoutOrNull(voteDuration+500L) {
                listenToRoom()
                    .flowOn(Dispatchers.IO)
                    .first { it.votes.size == forTests(it.playersList()) }.let {
                        Log.d(tag, "Got all votes")
                        nextRound(it)
                    }
            }
            if (result == null) {
                Log.d(tag, "Timeout reached")
                nextRound(listenToRoom().first())
            }
        }
    }

    private fun forTests(players: ArrayList<Player>): Int {
        return if (players.size <= 5) 1 else 2

    }

    private fun sizeOfPlaying(players: ArrayList<Player>): Int {
        val filtered = players.filter { !it.votedOut }
        return filtered.size
    }

    private fun nextRound(room: Room) {
        Log.d(tag, "Getting ready on next round")
        val maxKey = maxKeyOrNull(room)
        db.runBatch { batch ->
            if (maxKey != null) {
                batch.update(roomRef, "players.$maxKey.votedOut", true)
                batch.update(roomRef, "lastOut", room.players[maxKey]!!.name)
                Log.d(tag, "${room.players[maxKey]!!.name} was voted out")
                if (gameEnded(room, maxKey)) {
                    batch.update(roomRef, "state", RoomState.ended)
                    Log.d(tag, "All blancos voted out")
                } else {
                    batch.update(roomRef, "state", RoomState.playing)
                    batch.update(roomRef, "round", FieldValue.increment(1))
                }
            } else {
                batch.update(roomRef, "lastOut", "No one")
                batch.update(roomRef, "state", RoomState.playing)
                batch.update(roomRef, "round", FieldValue.increment(1))
                Log.d(tag, "No one was voted out")
            }
            batch.update(roomRef, "votes", hashMapOf<String, Int>())
        }.addOnCompleteListener {
            Log.d(tag, "Success on Next Round ${System.currentTimeMillis()}")
        }.addOnFailureListener {
            Log.d(tag, "Error on Next Round")
        }
    }

    private fun maxKeyOrNull(room: Room): String? {
        val voteCount = HashMap<String, Int>()
        for (vote in room.votes.values) {
            val count = voteCount[vote] ?: 0
            voteCount[vote] = count + 1
        }
        var maxKey: String? = null
        var maxValue = 0
        for (vote in voteCount) {
            if (vote.value == maxValue) {
                maxKey = null
            }
            if (vote.value > maxValue) {
                maxKey = vote.key
                maxValue = vote.value
            }
        }
        return maxKey
    }

    private fun gameEnded(room: Room, votedUid: String): Boolean {
        room.players[votedUid]!!.votedOut = true
        val blancoPlayers = room.players.values.filter { it.blanco }
        Log.d(tag, "$blancoPlayers")
        return if (!blancoPlayers.all { it.votedOut }) {
            val playingBlancos = blancoPlayers.filter { !it.votedOut }
            val playingNormals = room.players.values.filter { !it.host && !it.votedOut && !it.blanco}
            playingBlancos.size >= playingNormals.size
        } else {
            true
        }
    }

    fun setWordTrigger(sentWord: String) {
        Log.d(tag, "Trigger fired: $sentWord")
        viewModelScope.launch {
            _word.emit(sentWord)
        }
    }

    fun setFields(word: String, players: HashMap<String, Player>, blancos: ArrayList<Player>) {
        val mPlayers = modifyPlayers(players, blancos)
        db.runBatch { batch ->
            batch.update(roomRef, "word", word)
            batch.update(roomRef, "state", RoomState.firstRound)
            batch.update(roomRef, "players", mPlayers)
            batch.update(roomRef, "voteDuration", voteDuration)
        }.addOnCompleteListener {
            Self.listeners.forEach {
                Log.e(tag, it.toString())
            }
            Log.i(tag, "Initialized fields")
        }.addOnFailureListener {
            Log.i(tag, "Could not Initialize fields")
        }
    }

    private fun modifyPlayers(players: HashMap<String, Player>, blancos: ArrayList<Player>): HashMap<String, Player> {
        val hostPlayer = players.values.find { it.host }
        val nonHostPlayers = players.values.filter { it != hostPlayer }
        val nBlancos = numberOfBlancos(nonHostPlayers.size)
        val chosenBlancos = if (roomSettings.automatic) {
            nonHostPlayers.shuffled().take(nBlancos)
        } else {
            blancos
        }
        Log.d(tag, players.toString())
        chosenBlancos.forEach { it.blanco = true }
        Log.d(tag, "Selected random Blancos: $chosenBlancos")
        val orderList = (1..nonHostPlayers.size).shuffled()
        Log.d(tag, "Selected random order: $orderList")
        for ((index, player) in nonHostPlayers.withIndex()) {
            player.order = orderList[index]
        }
        Log.d(tag, players.toString())
        return players
    }

    private fun numberOfBlancos(n: Int): Int {
        val x = 5
        return when {
            n <= x -> 1
            n in (x+1)..(x+2) -> 2
            n in (x+3)..(x+4) -> 3
            n in (x+5)..(x+7) -> 4
            n >= (x+8) -> 5
            else -> 1
        }
    }

    fun resetRoom(room: Room) {
        room.players.values.forEach {
            it.blanco = false
            it.votedOut = false
        }
        val updates = hashMapOf<String, Any?>(
            "state" to RoomState.open,
            "players" to room.players,
            "round" to 1,
        )
        roomRef.update(updates)
            .addOnSuccessListener {
                Log.d(tag, "######## < Room re-created! > ########")
            }.addOnFailureListener { e ->
                Log.e(tag, "Error re-creating Room", e)
            }
    }

    fun populate() {
        val generatedPlayers: HashMap<String, Player> = hashMapOf(
            Self.uid!! to Self.playerObject(),
            "JOHNjohnjohn1" to Player("JOHNjohnjohn1", "john"),
            "MIKEmikemike2" to Player("MIKEmikemike2", "mike"),
            "MARYmarymary3" to Player("MARYmarymary3", "mary"),
            "LEAHleahleah4" to Player("LEAHleahleah4", "leah")
        )
        val extraPlayers: HashMap<String, Player> = hashMapOf(
            "BOBbobbob" to Player("BOBbobbob", "bob"),
            "ZOEzoezoe" to Player("ZOEzoezoe", "zoe"),
            "GOBgobgob" to Player("GOBgobgob", "gob"),
            "TOTtottot" to Player("TOTtottot", "tot"),
            "ATReusfer" to Player("ATReusfer", "Atreus Ferragor")
        )
        if (extra) {
            val alle = generatedPlayers + extraPlayers
            roomRef.update("players", alle)
        } else {
            roomRef.update("players", generatedPlayers)
            extra = true
        }
        Log.d(tag, "Generated players")
    }

}
