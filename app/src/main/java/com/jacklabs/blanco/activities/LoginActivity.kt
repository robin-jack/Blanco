package com.jacklabs.blanco.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jacklabs.blanco.R
import com.jacklabs.blanco.Self
import com.jacklabs.blanco.databinding.ActivityLogInBinding
import com.jacklabs.blanco.viewmodels.LoginViewModel
import kotlinx.coroutines.flow.collect

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding
    private val model: LoginViewModel by viewModels()
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 1
    private val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

        binding.btGoogleLogIn.setOnClickListener {
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        binding.btEmailLogIn.setOnClickListener {
            Log.d(tag, "login with email")
            val email = binding.etEmail.text
            val pass = binding.etPassword.text
            if (email.isNotEmpty() and pass.isNotEmpty()) {
                model.emailLogin(
                    email.toString(),
                    pass.toString()
                )
            } else {
                when {
                    email.isEmpty() -> Toast.makeText(baseContext, "Please fill out your email to sign in", Toast.LENGTH_SHORT).show()
                    pass.isEmpty() -> Toast.makeText(baseContext, "Please fill out your password to sign in", Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            model.loginState.collect {
                when(it) {
                    is LoginViewModel.LoginState.Success -> {
                        updateUI(it.user)
                        binding.pbLoading.isVisible = false
                    }
                    is LoginViewModel.LoginState.Error -> {
                        updateUI(null)
                        binding.pbLoading.isVisible = false
                    }
                    is LoginViewModel.LoginState.Loading -> {
                        binding.pbLoading.isVisible = true
                    }
                    else -> Unit
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                Log.d(tag, "Google sign in user: $account.id")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(tag, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.pbLoading.isVisible = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithCredential: success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithCredential: failure", task.exception)
                    updateUI(null)
                }
                binding.pbLoading.isVisible = false
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(tag, "Signed in as user: $user")
             Self.uid = user.uid
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent) // TO MAIN
            this.finish()
        } else {
            Log.d(tag, "Please sign in")
            Toast.makeText(this, "Email or password incorrect", Toast.LENGTH_LONG).show()
        }
    }
}