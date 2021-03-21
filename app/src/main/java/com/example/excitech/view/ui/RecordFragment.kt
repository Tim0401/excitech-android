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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.excitech.R
import com.example.excitech.viewModel.RecordViewModel
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions


const val TAG_OF_RECORD_FRAGMENT = "RecordFragment"

@RuntimePermissions
class RecordFragment : Fragment() {

    companion object {
        fun newInstance() = RecordFragment()
    }

    private lateinit var viewModel: RecordViewModel
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
        viewModel = ViewModelProvider(this, RecordViewModel.Factory(
            requireActivity().application
        )).get(RecordViewModel::class.java)
        viewModel.isRecordingLiveData.observe(viewLifecycleOwner, { isRecording ->
            isRecording?.let {
                if (it) {
                    recordButton.setImageResource(R.drawable.stop)
                } else {
                    recordButton.setImageResource(R.drawable.record)
                }
            }
        })

        viewModel.recordingDurationLiveData.observe(viewLifecycleOwner, { duration ->
            duration?.let {
                val hour = duration / (60 * 60)
                val minute = duration / 60
                val second = duration % 60
                recordingDuration.text = "%02d:%02d:%02d".format(hour, minute, second)

            }
        })
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    fun recordSound(){
        viewModel.onRecord()
    }

}