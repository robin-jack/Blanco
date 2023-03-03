package com.jacklabs.blanco.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.jacklabs.blanco.DB
import com.jacklabs.blanco.R
import com.jacklabs.blanco.databinding.ActivityCodeInputBinding

class CodeInputActivity : BaseActivity() {

    private lateinit var binding: ActivityCodeInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodeInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setBackgroundDrawableResource(R.drawable.activity_main_bg)
        //X1 val playerName = intent.getStringExtra("playerName")

        binding.etCodeInput.requestFocus()

        binding.btJoinRoom.setOnClickListener {
            val hostName = binding.etCodeInput.text
            if (hostName.isNotEmpty()) {
                val intent = Intent(this, RoomActivity::class.java)
                DB.joinGame("aaaa", hostName.toString()) // JOIN GAME //playerName.toString()
                startActivity(intent) // TO ROOM
            } else Toast.makeText(this, "Enter room code to join!", Toast.LENGTH_SHORT).show()
        }

        binding.btBack.setOnClickListener {
            finish()
        }

    }

}