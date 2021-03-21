package com.example.excitech.view.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.example.excitech.R
import com.example.excitech.databinding.PlayerFragmentBinding
import com.example.excitech.viewModel.PlayerViewModel

class PlayerFragment : Fragment() {

    companion object {
        private const val KEY_AUDIO_ID = "audio_id"

        fun forAudio(audioId: String) = PlayerFragment().apply {
            arguments = Bundle().apply { putString(KEY_AUDIO_ID, audioId) }
        }
    }

    private val audioId by lazy {
        requireNotNull(
                arguments?.getString(KEY_AUDIO_ID)
        ) {
            "audioId must not be null"
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this, PlayerViewModel.Factory(
                requireActivity().application, audioId
        )).get(PlayerViewModel::class.java)
    }

    private lateinit var binding: PlayerFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.player_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            playerViewModel = viewModel
        }

        viewModel.audioLiveData.observe(viewLifecycleOwner, Observer { audio ->
            audio?.let {
                viewModel.setAudio(it)
            }
        })
    }

}