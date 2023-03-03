package com.jacklabs.blanco

import android.view.View
import com.google.android.material.snackbar.Snackbar

sealed class CreateState {
    object Success: CreateState()
    class Error(private val message: String): CreateState() {
        fun showError(root: View) {
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
        }
    }
    object Loading: CreateState()
    object Empty: CreateState()
}