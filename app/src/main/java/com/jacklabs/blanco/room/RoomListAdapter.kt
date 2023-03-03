package com.jacklabs.blanco.room

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.jacklabs.blanco.R
import com.jacklabs.blanco.databinding.RoomRowItemBinding
import java.security.AccessController.getContext

class RoomListAdapter(private val roomList: ArrayList<Room>) :
    RecyclerView.Adapter<RoomListAdapter.ViewHolder>(){

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = RoomRowItemBinding.inflate(LayoutInflater
            .from(parent.context), parent, false)

        return ViewHolder(parent.context, view, mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room: Room = roomList[position]
        holder.bind(room)
    }

    override fun getItemCount() = roomList.size

    inner class ViewHolder(private val context: Context, private val view: RoomRowItemBinding, listener: OnItemClickListener) : RecyclerView.ViewHolder(view.root) {
        fun bind(room: Room) = with(view) {
            val text = "${room.host}'s room"
            val spannableString = SpannableString(text)
            spannableString.setSpan(ForegroundColorSpan(Color.BLACK), 0, room.host!!.length, 0)
            tvHostName.text = spannableString
            tvNumberOfPlayers.text = room.players.size.toString()
            if (room.state != RoomState.open) {
                tvOpen.text = "Closed"
                tvOpen.setTextColor(Color.RED)
            } else {
                tvOpen.text = "Open"
                tvOpen.setTextColor(getColor(context, R.color.green))
            }
        }

        init {
            view.cvRoom.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }

    }
}
