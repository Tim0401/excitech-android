package com.example.excitech.view.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.example.excitech.R
import com.example.excitech.databinding.AudioListFragmentBinding
import com.example.excitech.databinding.AudioListItemBinding
import com.example.excitech.service.model.Audio
import com.example.excitech.view.adapter.AudioAdapter
import com.example.excitech.view.callback.AudioClickCallback
import com.example.excitech.viewModel.AudioListViewModel
import com.example.excitech.viewModel.RecordViewModel

class AudioListFragment : Fragment() {

    companion object {
        fun newInstance() = AudioListFragment()
    }

    private lateinit var viewModel: AudioListViewModel
    private lateinit var binding: AudioListFragmentBinding
    private val audioAdapter: AudioAdapter = AudioAdapter(object : AudioClickCallback {
        override fun onClick(audio: Audio) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && activity is MainActivity) {
                (activity as MainActivity).show(audio)
            }
        }
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.audio_list_fragment, container, false) //dataBinding
        binding.apply {
            audioList.adapter = audioAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(AudioListViewModel::class.java)
        viewModel.audioListLiveData.observe(viewLifecycleOwner, Observer { projects ->
            projects?.let {
                audioAdapter.setAudioList(it)
            }
        })
    }

}