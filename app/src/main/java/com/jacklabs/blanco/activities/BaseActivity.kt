package com.jacklabs.blanco.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.databinding.ActivityMainBinding

open class BaseActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private val tag = "BaseActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun currentUserCheckLogin() {
        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        checkUserLogin(currentUser)
    }

    private fun checkUserLogin(user: FirebaseUser?) {
        if (user != null) {
            Self.uid = user.uid
            Log.d(tag, "Logged in as $user")
            val rootv = findViewById<View>(android.R.id.content)
            Snackbar.make(rootv, "Logged in as ${user.email}", Snackbar.LENGTH_LONG).show()
        } else {
            Log.d(tag, "user not signed in")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }

    override fun onResume() {
        super.onResume()
        currentUserCheckLogin()
    }
}