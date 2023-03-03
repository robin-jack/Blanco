package com.jacklabs.blanco.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jacklabs.blanco.Self

class ExitRoomFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit this room?")
            .setPositiveButton("Yes") { _,_ -> exitRoom() }
            .setNegativeButton("Cancel") { _,_ -> }
            .create()

    private fun exitRoom() {
        val db = FirebaseFirestore.getInstance()
        if (Self.host) {
            Log.d(tag, "Deleting room!")
            db.collection("games").document(Self.roomId!!)
                .delete()
        } else {
            if (arguments?.getString(ARG_NAME) == "open") {
                Log.d(tag, "Deleting self from room!")
                val docRef = db.collection("games").document(Self.roomId!!)
                docRef.update(hashMapOf<String, Any>(Self.uid!! to FieldValue.delete()))
            }
        }
        activity?.finish()
    }

    companion object {
        private const val ARG_NAME = "state"

        @JvmStatic
        fun newInstance(state: String) =
            ExitRoomFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, state)
                }
            }
    }
}