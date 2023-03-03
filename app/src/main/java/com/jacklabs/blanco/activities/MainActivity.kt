package com.jacklabs.blanco.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.jacklabs.blanco.CreateState
import com.jacklabs.blanco.R
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.Self.roomId
import com.jacklabs.blanco.Self.uid
import com.jacklabs.blanco.databinding.ActivityMainBinding
import com.jacklabs.blanco.viewmodels.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect


@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private val model: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val sharedPrefs: SharedPreferences get() = getSharedPreferences("SharedPrefs", Context.MODE_PRIVATE)
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.d(tag, "Nice!")
                } else {
                    Log.d(tag, "error getting permissions")
                }
            }

        currentUserCheckLogin()     // see if logged in
        retrieveSelf()              // get Self
        //checkForDynamicLink()     // see if opened with link
        //checkLastGame()           // see last game
        //etPlayerName.setSelection(0) - how to set cursor at start with gravity center

        window.setBackgroundDrawableResource(R.drawable.activity_main_bg)

        Log.e(tag, System.currentTimeMillis().toString())
        Log.e(tag, System.currentTimeMillis().toString())
        Log.e(tag, System.currentTimeMillis().toString())

        lifecycleScope.launchWhenStarted {
            model.createState.collect {
                when(it) {
                    is CreateState.Success -> {
                        val intent = Intent(this@MainActivity, RoomHostActivity::class.java)
                        startActivity(intent) // TO ROOM HOST
                        binding.pbLoading.isVisible = false
                    }
                    is CreateState.Error -> {
                        it.showError(binding.root)
                        binding.pbLoading.isVisible = false
                    }
                    is CreateState.Loading -> {
                        binding.pbLoading.isVisible = true
                    }
                    else -> Unit
                }
            }
        }

        binding.btCreate.setOnClickListener {
            val playerName = binding.etPlayerName.text
            if (playerName.isNotEmpty()) {
                Self.name = playerName.toString()
                Self.host = true
                model.createGame(playerName.toString())
            } else Toast.makeText(this, "Enter your name to play!", Toast.LENGTH_SHORT).show()
        }

        binding.btJoin.setOnClickListener {
            val playerName = binding.etPlayerName.text
            if (playerName.isNotEmpty()) {
                Self.name = playerName.toString()
                Self.host = false
                val intent = Intent(this, RoomListActivity::class.java)
                startActivity(intent) // TO ROOM LIST
            } else Toast.makeText(this, "Enter your name to play!", Toast.LENGTH_SHORT).show()
        }

        // TEST BUTTONS ------------------------------------------------------>

        binding.btTest.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.btTest2.setOnClickListener {
            val msg = "you are gay"
            Firebase.dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.SHORT) {
                link = Uri.parse("https://jacklabs.page.link?msg=${msg}")
                domainUriPrefix = "https://jacklabs.page.link"
                androidParameters { }
                socialMetaTagParameters {
                    title = "Play a round of Blanco!"
                    description = "Blanco is the best game in the universe according to its creator"
                    imageUrl = Uri.parse("https://instagram.fxry1-1.fna.fbcdn.net/v/t51.2885-15/sh0.08/e35/s640x640/32566786_995256690642332_2005120660552024064_n.jpg?_nc_ht=instagram.fxry1-1.fna.fbcdn.net&_nc_cat=108&_nc_ohc=ofD45hgZKkEAX-95uzf&edm=AP_V10EBAAAA&ccb=7-4&oh=00_AT-thPrMw9Phc3PB69cr2K2ewOZ9Rc4HEPQmAy4EW4l11g&oe=61CB79AD&_nc_sid=4f375e")
                }
            }.addOnSuccessListener { (link, flowchartlink) ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${Self.name} invited you to join to play $link")
                    Log.d(tag, "whaaa??? -> $flowchartlink")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }.addOnFailureListener { e ->
                Log.w(tag, "Error creating link", e)
                Toast.makeText(this, "Error creating link", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btTest3.setOnClickListener {
            val gso = GoogleSignInOptions.Builder().build()
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mGoogleSignInClient.signOut()
            firebaseAuth.signOut()
            Self.resetSelf()
            checkUserLogin(null)
            Log.d(tag, "Signed Out")
        }

        binding.btTest4.setOnClickListener {
            Log.d(tag, "A")
        }

        // ------------------------------------------------------------------->
    }

    private fun checkLastGame() {
        if (roomId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("games").document(roomId!!).get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        // show fragment
                    }
                }
        }
    }

    private fun checkForDynamicLink() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                    val msg = deepLink?.getQueryParameter("msg")
                    Log.d(tag, msg.toString())
                    Log.d(tag, "Gugu gaga $deepLink")
                }
            }
            .addOnFailureListener(this) { e -> Log.w(tag, "getDynamicLink: onFailure", e) }
    }

    private fun currentUserCheckLogin() {
        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        checkUserLogin(currentUser)
    }

    private fun checkUserLogin(user: FirebaseUser?) {
        if (user != null) {
            uid = user.uid
            Log.d(tag, "Logged in as $user")
            Snackbar.make(binding.root, "Logged in as ${user.email}", Snackbar.LENGTH_LONG).show()
        } else {
            Log.d(tag, "user not signed in")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }

    private fun saveSelf() {
        val editor = sharedPrefs.edit()
        editor.putString("name", Self.name)
        editor.putString("roomRef", roomId)
        editor.apply()
        Log.i(tag, "Saved Self")
    }

    private fun retrieveSelf() {
        Self.name = sharedPrefs.getString("name", null)
        roomId = sharedPrefs.getString("roomRef", null)
        binding.etPlayerName.setText(Self.name)
        Log.i(tag, "Retrieved Self")
    }

    override fun onResume() {
        super.onResume()
        currentUserCheckLogin()
    }

    override fun onPause() {
        super.onPause()
        saveSelf()
    }

}