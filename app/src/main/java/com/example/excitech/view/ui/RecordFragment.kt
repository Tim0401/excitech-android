package com.example.excitech.view.ui

import android.Manifest
import android.R.attr.button
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.excitech.R
import com.example.excitech.viewModel.RecordViewModel
import dagger.hilt.android.AndroidEntryPoint
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions


const val TAG_OF_RECORD_FRAGMENT = "RecordFragment"

@RuntimePermissions
@AndroidEntryPoint
class RecordFragment : Fragment() {

    companion object {
        fun newInstance() = RecordFragment()
    }

    private val viewModel: RecordViewModel by viewModels()
    private lateinit var recordButton: ImageButton
    private lateinit var recordingDuration: TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.record_fragment, container, false)
        recordButton = view.findViewById(R.id.recordButton)
        recordingDuration = view.findViewById(R.id.time)

        // ボタンで録音開始
        recordButton.setOnClickListener{
            this.recordSoundWithPermissionCheck()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isRecordingLiveData.observe(viewLifecycleOwner, { isRecording ->
            isRecording?.let {
                if (it) {
                    recordButton.setImageResource(R.drawable.stop)
                } else {
                    recordButton.setImageResource(R.drawable.record)
                }
            }
        })

        viewModel.recordingDurationLiveData.observe(viewLifecycleOwner, { durationText ->
            durationText?.let {
                recordingDuration.text = durationText

            }
        })
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun recordSound(){
        viewModel.startOrStopRecording()
    }

}