package com.jacklabs.blanco.fragments


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.jacklabs.blanco.databinding.FragmentWordInputBinding
import com.jacklabs.blanco.showKeyboard
import com.jacklabs.blanco.viewmodels.RoomHostViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class WordInputFragment : DialogFragment() {

    private val model: RoomHostViewModel by activityViewModels()
    private var _binding: FragmentWordInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = FragmentWordInputBinding.inflate(inflater, container, false)

        dialog?.window!!.setGravity(Gravity.BOTTOM)
        dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btSendWord.setOnClickListener {
            setWord()
        }

        binding.etWordInput.setOnEditorActionListener { _, keyCode, event ->
            if (((event?.action ?: -1) == KeyEvent.ACTION_DOWN) || keyCode == EditorInfo.IME_ACTION_NEXT) {
                setWord()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        return binding.root

    }

    @ExperimentalCoroutinesApi
    private fun setWord() {
        val word = binding.etWordInput.text
        if (word.isNotEmpty() || word != null) {
            model.setWordTrigger(word.toString())
            dismiss()
        } else {
            Toast.makeText(activity, "Please type in a secret word!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.etWordInput.postDelayed({ binding.etWordInput.showKeyboard(requireActivity())}, 50)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}