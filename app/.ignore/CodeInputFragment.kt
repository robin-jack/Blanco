package com.jacklabs.blanco.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jacklabs.blanco.MySingleton
import com.jacklabs.blanco.activities.RoomActivity
import com.jacklabs.blanco.databinding.FragmentCodeInputBinding
import com.jacklabs.blanco.showKeyboard

class CodeInputFragment : DialogFragment() {

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private var _binding: FragmentCodeInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = FragmentCodeInputBinding.inflate(inflater, container, false)

        dialog?.window!!.setGravity(Gravity.BOTTOM)
        dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btJoinRoom.setOnClickListener {
            checkToJoinRoom()
        }

        binding.etCodeInput.setOnEditorActionListener { _, keyCode, event ->
            if (((event?.action ?: -1) == KeyEvent.ACTION_DOWN) || keyCode == EditorInfo.IME_ACTION_NEXT) {
                checkToJoinRoom()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        return binding.root

    }

    override fun onResume() {
        super.onResume()
        dialog?.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.etCodeInput.postDelayed({ binding.etCodeInput.showKeyboard(requireActivity())}, 50)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkToJoinRoom(){
        val playerName = arguments?.getString(ARG_NAME)
        val code = binding.etCodeInput.text
        if (code.isNotEmpty()) {
            MySingleton.player.name = playerName
            MySingleton.player.host = false
            MySingleton.code = code.toString().toLong()

            val game = db.collection("games")
            game.whereEqualTo("code", code.toString().toLong()).whereEqualTo("started", false)
                .get()
                .addOnSuccessListener { docs ->
                    if (docs != null) {
                        val doc = docs.first()
                        if (doc.data["host"] == MySingleton.player.uid) {
                            MySingleton.player.host = true
                        }
                        // JOIN ROOM
                        game.document(doc.id).update("players", FieldValue.arrayUnion(MySingleton.player))
                        val intent = Intent(activity, RoomActivity::class.java)
                        startActivity(intent) // TO ROOM
                        Log.d(TAG, "Game joined!")
                    } else Log.d(TAG, "Game does not exist :(")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error joining Game :(", e)
                }
        } else Toast.makeText(activity, "Enter room code to join!", Toast.LENGTH_SHORT).show()
    }

    private fun testToJoinRoom(){
        val playerName = arguments?.getString(ARG_NAME)
        val code = binding.etCodeInput.text
        if (code.isNotEmpty()) {
            MySingleton.player.name = playerName
            MySingleton.player.host = false
            MySingleton.code = code.toString().toLong()

            val game = db.collection("players").document(MySingleton.playersRef!!)
            game.get()
                .addOnSuccessListener { doc ->
                    if (doc.data!!["host"] == MySingleton.player.uid) {
                        MySingleton.player.host = true
                    }
                    // JOIN ROOM
                    game.update("players", FieldValue.arrayUnion(MySingleton.player))
                    val intent = Intent(activity, RoomActivity::class.java)
                    startActivity(intent) // TO ROOM
                    Log.d(TAG, "Game joined!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error joining Game :(", e)
                }
        } else Toast.makeText(activity, "Enter room code to join!", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ARG_NAME = "player_name"
        const val TAG = "CodeInputActivity"

        @JvmStatic
        fun newInstance(name: String) =
            CodeInputFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                }
            }
    }
}