package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.excitech.service.model.Audio
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AudioListViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    var audioListLiveData: MutableLiveData<List<Audio>> = MutableLiveData()
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    init {
        loadAudioList()
    }

    private fun loadAudioList() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {

            val files = context.filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".3gp") } ?: listOf<File>()
            val mmr = MediaMetadataRetriever()

            val audioList = files.map{
                mmr.setDataSource(it.path)
                val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val duration = durationMs / 1000
                val h = duration / 3600
                val m = (duration - h * 3600) / 60
                val s = duration - (h * 3600 + m * 60)
                val durationText = "%02d:%02d:%02d".format(h, m, s)


                Audio(it.name, durationText, SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(it.lastModified()))
            }
            audioListLiveData.postValue(audioList)
        } catch (e: Exception){
            e.stackTrace
        }
    }
}