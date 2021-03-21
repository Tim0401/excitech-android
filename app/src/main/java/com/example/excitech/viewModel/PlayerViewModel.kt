package com.example.excitech.viewModel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.example.excitech.service.model.Audio
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlayerViewModel(application: Application, private val audioId: String) : AndroidViewModel(application) {
    val audioLiveData: MutableLiveData<Audio> = MutableLiveData()
    var audio = ObservableField<Audio>()

    private val context = getApplication<Application>().applicationContext

    init {
        loadAudio()
    }

    private fun loadAudio() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {
            val filePath = context.filesDir.path + '/' + audioId
            val file = File(context.filesDir.path, audioId)

            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            val duration = durationMs / 1000
            val h = duration / 3600
            val m = (duration - h * 3600) / 60
            val s = duration - (h * 3600 + m * 60)
            val durationText = "%02d:%02d:%02d".format(h, m, s)

            audioLiveData.postValue(Audio(audioId,durationText, SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(file.lastModified())))

        } catch (e: Exception) {
            Log.e("loadProject:Failed", e.stackTrace.toString())
        }
    }


    fun setAudio(audio: Audio) {
        this.audio.set(audio)
    }

    class Factory(private val application: Application, private val soundId: String) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application, soundId) as T
        }
    }
}