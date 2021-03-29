package com.example.excitech.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.example.excitech.R
import com.example.excitech.databinding.PlayerFragmentBinding
import com.example.excitech.viewModel.PlayerViewModel
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
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: ImageButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.player_fragment, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        seekBar = binding.root.findViewById(R.id.seekBar)
        playPauseButton = binding.root.findViewById(R.id.playPause)
        seekBar.progress = 0
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadAudio()
        binding.apply {
            playerViewModel = viewModel
        }
        // 現在の再生位置
        viewModel.audioLiveData.observe(viewLifecycleOwner, { audio ->
            audio?.let {
                seekBar.max = audio.durationMs.toInt()
            }
        })
        // シークバーの位置調整
        viewModel.currentTimeMsLiveData.observe(viewLifecycleOwner, { ms ->
            ms?.let {
                seekBar.progress = ms
            }
        })
        // 再生ボタンの表示切り替え
        viewModel.isPlayingLiveData.observe(viewLifecycleOwner, { isPlaying ->
            isPlaying?.let {
                if (isPlaying){
                    playPauseButton.setImageResource(R.drawable.exo_ic_pause_circle_filled)
                } else {
                    playPauseButton.setImageResource(R.drawable.exo_ic_play_circle_filled)
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        viewModel.unsubscribe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.subscribe()
    }
}