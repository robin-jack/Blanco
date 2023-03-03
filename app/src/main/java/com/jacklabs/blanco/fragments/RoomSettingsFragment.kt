package com.jacklabs.blanco.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.jacklabs.blanco.R
import com.jacklabs.blanco.databinding.FragmentRoomSettingsBinding
import com.jacklabs.blanco.room.RoomSettings

class RoomSettingsFragment : DialogFragment() {

    private var _binding: FragmentRoomSettingsBinding? = null
    private val binding get() = _binding!!
    private var listener: GameSettings? = null

    var mc = 0
    var modes = arrayOf("classic", "classic+", "deluxe")

    var automatic = true

    var numBlancos = 1
    var maxBlancos = 5

    var tc = 2
    var time = arrayOf(10, 15, 20, 25, 30)

    companion object {
        fun newInstance(rs: RoomSettings): RoomSettingsFragment {
            val fragment = RoomSettingsFragment()
            val args = Bundle()
            args.putString("mode", rs.mode)
            args.putBoolean("automatic", rs.automatic)
            args.putInt("numBlancos", rs.numBlancos)
            args.putInt("time", rs.time)
            fragment.arguments = args
            return fragment
        }
    }

    interface GameSettings {
        fun applySettings(roomSettings: RoomSettings)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        _binding = FragmentRoomSettingsBinding.inflate(inflater, container, false)

        arguments?.let {
            mc = modes.indexOf(it.getString("mode"))
            automatic = it.getBoolean("automatic")
            numBlancos = it.getInt("numBlancos")
            tc = time.indexOf(it.getInt("time"))
            binding.btMode.text = modes[mc]
            binding.tvBl.text = numBlancos.toString()
            binding.tvTime.text = time[tc].toString()
        }

        binding.btMode.setOnClickListener {
            mc = (mc+1) % modes.size
            binding.btMode.text = modes[mc]
        }

        binding.btBlancos.setOnClickListener{
            automatic = !automatic
            if (automatic) {
                binding.btBlancos.text = "automatic"
            } else {
                binding.btBlancos.text = "manual"
            }
        }

        binding.tvBlMinus.setOnClickListener {
            if (numBlancos > 1) {
                numBlancos -= 1
                binding.tvBl.text = numBlancos.toString()
            }
        }
        binding.tvBlPlus.setOnClickListener {
            if (numBlancos < maxBlancos) {
                numBlancos += 1
                binding.tvBl.text = numBlancos.toString()
            }
        }

        binding.tvTimeMinus.setOnClickListener {
            if (tc > 0) {
                tc -= 1
                binding.tvTime.text = getString(R.string.vote_time, time[tc])
            }
        }
        binding.tvTimePlus.setOnClickListener {
            if (tc < (time.size-1)) {
                tc += 1
                binding.tvTime.text = getString(R.string.vote_time, time[tc])
            }
        }

        binding.btApply.setOnClickListener {
            val roomSettings = RoomSettings(modes[mc], automatic, numBlancos, time[tc])
            listener?.applySettings(roomSettings)
            val manager = requireActivity().supportFragmentManager
            manager.beginTransaction().remove(this).commit()
        }


        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? GameSettings
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}