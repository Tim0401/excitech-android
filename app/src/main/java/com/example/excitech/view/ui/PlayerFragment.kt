package com.example.excitech.view.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.excitech.R
import com.example.excitech.databinding.PlayerFragmentBinding
import com.example.excitech.service.MusicService
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.player_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadAudio()
        binding.apply {
            playerViewModel = viewModel
        }
        viewModel.audioLiveData.observe(viewLifecycleOwner, { audio ->
            audio?.let {}
        })
    }
}