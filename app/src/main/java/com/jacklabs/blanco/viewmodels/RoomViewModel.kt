package com.jacklabs.blanco.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.firestore.*
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.databinding.ActivityRoomBinding
import com.jacklabs.blanco.room.Room
import com.jacklabs.blanco.room.RoomState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.lang.NullPointerException

@ExperimentalCoroutinesApi
open class RoomViewModel: ViewModel() {

    protected var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    protected var roomRef: DocumentReference = db.collection("games").document(Self.roomId!!)

    lateinit var listener: ListenerRegistration
    lateinit var roomRaw: Room

    var voteDuration: Long = 10 * 1000L
    private val voteDelay = 3

    private var tag = "RoomViewModel"

    @ExperimentalCoroutinesApi
    fun listenToRoom(): Flow<Room> = callbackFlow {
        listener = roomRef
            .addSnapshotListener() { snapshot, e ->
                if (e != null) {
                    Log.w(tag, "Listen to players failed", e)
                    cancel(message = "Error fetching players", cause = e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val roomSnap = snapshot.toObject(Room::class.java)!!
                    Log.i(tag, ">>Sending data ·")
                    roomRaw = roomSnap
                    trySend(roomSnap)
                }
            }

        Log.i(tag, "Opening room listener: $listener")
        awaitClose {
            listener.remove()
            Log.i(tag, "Closing room listener: $listener ")
        }

    }

    fun startVoteCountdown(timeLeft: Int, job: Job): Flow<String> {
        return flow {
            Log.d(tag, "Started VoteCountdown")
            delay(voteDelay*1000L)
            var duration = timeLeft - voteDelay
            while (duration >= 0 && job.isActive) {
                emit(duration.toString())
                delay(1000L)
                duration -= 1
            }
        }.flowOn(Dispatchers.Main)
    }



    // test
    fun castVote(uid: String?, name: String) {
        roomRef.update("votes.${Self.uid}", uid)
        Log.d(tag, "Casted vote: $name")
    }

}