package com.jacklabs.blanco.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel: ViewModel() {

    private lateinit var auth: FirebaseAuth
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Empty)
    internal val loginState: StateFlow<LoginState> = _loginState

    private val tag = "LoginViewModel"

    sealed class LoginState {
        data class Success(val user: FirebaseUser?): LoginState()
        data class Error(val message: String): LoginState()
        object Loading: LoginState()
        object Empty: LoginState()
    }

    fun emailLogin(email: String, password: String) = viewModelScope.launch {
        _loginState.value = LoginState.Loading
        auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "signInWithEmail: success")
                    _loginState.value = LoginState.Success(auth.currentUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "signInWithEmail: failure", task.exception)
                    _loginState.value = LoginState.Error("Authentication failed")
                }
            }
    }

}