package com.example.excitech.view.adapter

import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.excitech.R
import com.example.excitech.databinding.AudioListItemBinding
import com.example.excitech.service.model.Audio
import com.example.excitech.view.callback.AudioClickCallback

class AudioAdapter(private val audioClickCallback: AudioClickCallback?) :
        RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var audioList: List<Audio>? = null

    fun setAudioList(audioList: List<Audio>) {

        if (this.audioList == null) {
            this.audioList = audioList
            notifyItemRangeInserted(0, audioList.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return requireNotNull(this@AudioAdapter.audioList).size
                }

                override fun getNewListSize(): Int {
                    return audioList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldList = this@AudioAdapter.audioList
                    return oldList?.get(oldItemPosition) == audioList[newItemPosition]
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val audio = audioList[newItemPosition]
                    val old = audioList[oldItemPosition]
                    return audio.name == old.name
                }
            })
            this.audioList = audioList
            result.dispatchUpdatesTo(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewtype: Int): AudioViewHolder {
        val binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.audio_list_item, parent,
                false) as AudioListItemBinding
        binding.callback = audioClickCallback
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.binding.audio = audioList?.get(position)
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return audioList?.size ?: 0
    }

    open class AudioViewHolder(val binding: AudioListItemBinding) : RecyclerView.ViewHolder(binding.root)
}
