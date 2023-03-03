package com.jacklabs.blanco.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment

class ShowWordFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle("La palabra es:")
            .setMessage(arguments?.getString(ARG_NAME))
            .setPositiveButton("OK") { _,_ -> }
            .create()

    companion object {
        private const val ARG_NAME = "word"

        @JvmStatic
        fun newInstance(word: String) =
            ShowWordFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, word)
                }
            }
    }
}