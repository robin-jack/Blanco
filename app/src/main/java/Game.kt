import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.player.Player
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomSettings
import com.jacklabs.blanco.room.RoomState

object Game {

    private var automatic = true
    private const val tag = "Game"

    class Classic(roomSettings: RoomSettings) {

        private val tag = "Classic"

        var db: FirebaseFirestore = FirebaseFirestore.getInstance()
        var roomRef: DocumentReference = db.collection("games").document(Self.roomId!!)


        private fun setFields(word: String, players: HashMap<String, Player>) {
            val mPlayers = modifyPlayers(players)
            db.runBatch { batch ->
                batch.update(roomRef, "word", word)
                batch.update(roomRef, "state", RoomState.firstRound)
                batch.update(roomRef, "players", mPlayers)
                batch.update(roomRef, "voteDuration", 5)
            }.addOnCompleteListener {
                Self.listeners.forEach {
                    Log.e(tag, it.toString())
                }
                Log.i(tag, "Initialized fields")
            }.addOnFailureListener {
                Log.i(tag, "Could not Initialize fields")
            }
        }

        private fun modifyPlayers(players: HashMap<String, Player>): HashMap<String, Player> {
            val hostPlayer = players.values.find { it.host }
            val nonHostPlayers = players.values.filter { it != hostPlayer }
            val nBlancos = numberOfBlancos(nonHostPlayers.size)
            val chosenBlancos = nonHostPlayers.shuffled().take(nBlancos)
            chosenBlancos.forEach { it.blanco = true }
            Log.d(tag, "Selected random Blancos: $chosenBlancos")
            val orderList = (1..nonHostPlayers.size).shuffled()
            Log.d(tag, "Selected random order: $orderList")
            for ((index, player) in nonHostPlayers.withIndex()) {
                player.order = orderList[index]
            }
            return players
        }

    }

    class Deluxe(roomSettings: RoomSettings) {

        private val tag = "Deluxe"

        private fun modifyPlayers(players: HashMap<String, Player>): HashMap<String, Player> {

            val hostPlayer = players.values.find { it.host }
            val nonHostPlayers = players.values.filter { it != hostPlayer }
            val nBlancos = numberOfBlancos(nonHostPlayers.size)
            val chosenBlancos = if (automatic) automatic(players) else manual(players)
            //val chosenBlancos = nonHostPlayers.shuffled().take(nBlancos)
            //chosenBlancos.forEach { it.blanco = true }
            Log.d(tag, "Selected random Blancos: $chosenBlancos")
            val orderList = (1..nonHostPlayers.size).shuffled()
            Log.d(tag, "Selected random order: $orderList")
            for ((index, player) in nonHostPlayers.withIndex()) {
                player.order = orderList[index]
            }
            return players

        }

    }

    private fun automatic(players: HashMap<String, Player>): HashMap<String, Player> {
        return players
    }

    private fun manual(players: HashMap<String, Player>): HashMap<String, Player> {
        return players
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

}