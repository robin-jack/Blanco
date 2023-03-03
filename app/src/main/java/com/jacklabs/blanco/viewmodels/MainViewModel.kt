package com.jacklabs.blanco.viewmodels

import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.jacklabs.blanco.CreateState
import com.jacklabs.blanco.Self.playerObject
import com.jacklabs.blanco.Self.roomId
import com.jacklabs.blanco.Self.setAsOnline
import com.jacklabs.blanco.Self.uid
import com.jacklabs.blanco.player.Player
import com.jacklabs.blanco.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel: ViewModel() {

    private lateinit var db: FirebaseFirestore
    private val _createState = MutableStateFlow<CreateState>(CreateState.Empty)
    internal val createState: StateFlow<CreateState> = _createState
    private var tag = "MainViewModel"

    fun createGame(playerName: String) {
        _createState.value = CreateState.Loading
        db = FirebaseFirestore.getInstance()
        val room = Room(
            host = playerName,
            hostUid = uid!!,
            players = hashMapOf(uid!! to playerObject())
        )
        db.collection("games").add(room)
            .addOnSuccessListener {
                Log.d(tag, "######## < Room created! > ########")
                roomId = it.id
                _createState.value = CreateState.Success
            }.addOnFailureListener { e ->
                Log.w(tag, "Error creating Room", e)
                _createState.value = CreateState.Error("Error creating Room")
            }
    }

}