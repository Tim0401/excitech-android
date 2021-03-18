package com.example.excitech.viewModel

import android.R.attr.delay
import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.IOException
import java.util.*


private const val LOG_TAG = "AudioRecordTest"

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    var isRecordingLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    var recordingDurationLiveData: MutableLiveData<Int> = MutableLiveData(0)

    private val timer = Timer()
    private var fileName: String = "aaa.wav"
    private var recorder: MediaRecorder? = null
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    fun onRecord() {
        if(isRecordingLiveData.value == true){
            isRecordingLiveData.postValue(false)
            stopRecording()
        } else {
            isRecordingLiveData.postValue(true)
            startRecording()
        }
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(context.filesDir.path + '/' +fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            Log.d(LOG_TAG,context.filesDir.path + '/' +fileName)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()

            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    recordingDurationLiveData.postValue(recordingDurationLiveData.value!! + 1)
                }
            }, 1000, 1000)
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            reset()
            release()
        }
        timer.cancel()
        recordingDurationLiveData.postValue(0)
        recorder = null
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecordViewModel(application) as T
        }
    }

}