package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.*
import javax.inject.Inject


private const val LOG_TAG = "AudioRecordTest"

@HiltViewModel
class RecordViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    var isRecordingLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    var recordingDurationLiveData: MutableLiveData<String> = MutableLiveData(initialDurationText)

    companion object {
        const val initialDurationText = "00:00:00"
    }
    private var timer = Timer()
    private var duration = 0
    private var fileName: String = UUID.randomUUID().toString()
    private var recorder: MediaRecorder? = null
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    @MainThread
    fun startOrStopRecording() {
        if(isRecordingLiveData.value == true){
            // UIスレッド上であるなら早い メインスレッド以外から呼ばれるときはpostValue
            isRecordingLiveData.value = false
            stopRecording()
        } else {
            isRecordingLiveData.value = true
            startRecording()
        }
    }

    @MainThread
    private fun startRecording() {
        recorder = MediaRecorder().apply {
            fileName = UUID.randomUUID().toString()
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(context.filesDir.path + '/' + fileName + ".3gp")
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            Log.d(LOG_TAG,context.filesDir.path + '/' +fileName)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()

            timer = Timer()
            duration = 0
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    duration++
                    val hour = duration / (60 * 60)
                    val minute = duration / 60
                    val second = duration % 60
                    recordingDurationLiveData.postValue("%02d:%02d:%02d".format(hour, minute, second))
                }
            }, 1000, 1000)
        }
    }

    @MainThread
    private fun stopRecording() {
        recorder?.apply {
            stop()
            reset()
            release()
        }
        timer.cancel()
        recordingDurationLiveData.value = initialDurationText
        recorder = null
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecordViewModel(application) as T
        }
    }

}