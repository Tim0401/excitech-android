package com.example.excitech.view.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.excitech.R
import com.example.excitech.databinding.PlayerFragmentBinding
import com.example.excitech.viewModel.PlayerViewModel
import com.example.excitech.viewModel.RecordViewModel
import com.google.android.exoplayer2.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
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

    @Inject
    lateinit var playerViewModelAssistedFactory: PlayerViewModel.AssistedFactory
    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModel.provideFactory(playerViewModelAssistedFactory, audioId)
    }

    private lateinit var binding: PlayerFragmentBinding
    private lateinit var playerView: PlayerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.player_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        playerView = binding.root.findViewById(R.id.videoView)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            playerViewModel = viewModel
        }

        viewModel.audioLiveData.observe(viewLifecycleOwner, { audio ->
            audio?.let {}
        })

        viewModel.initPlayerLiveData.observe(viewLifecycleOwner, {  isInitialized ->
            isInitialized?.let {
                if(isInitialized){
                    playerView.player = viewModel.getPlayer()
                }
            }
        })
    }

}