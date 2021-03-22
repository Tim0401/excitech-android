package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.example.excitech.service.model.Audio
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PlayerViewModel(application: Application, private val audioId: String) : AndroidViewModel(application) {
    // シリアライズ可能
    val audioLiveData: MutableLiveData<Audio> = MutableLiveData()
    // シリアライズできない->どうするか？
    val playerLiveData: MutableLiveData<SimpleExoPlayer> = MutableLiveData()
    var audio = ObservableField<Audio>()

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    init {
        loadAudio()
    }

    private fun loadAudio() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {
            val filePath = context.filesDir.path + '/' + audioId
            val file = File(context.filesDir.path, audioId)

            // 別スレッドで実行 コルーチン
            val durationMs =  withContext(Dispatchers.IO) {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(filePath)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            }
            // メインスレッドで残りを実行
            val duration = durationMs / 1000
            val h = duration / 3600
            val m = (duration - h * 3600) / 60
            val s = duration - (h * 3600 + m * 60)
            val durationText = "%02d:%02d:%02d".format(h, m, s)

            audioLiveData.postValue(Audio(audioId, durationText, SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).format(file.lastModified())))

            val source = buildMediaSource(filePath)
            val exoPlayer = SimpleExoPlayer.Builder(context).build().apply {
                playWhenReady = playWhenReady
                setMediaSource(source)
                prepare()
            }

            // TODO: PlauerのLivedataを渡すのは危ない
            // 普通のインスタンスとして持って呼ぶ方がいい
            playerLiveData.postValue(exoPlayer)

            // onPauseのタイミングでplayerのpauseを呼ぶとかする

        } catch (e: Exception) {
            Log.e("loadProject:Failed", e.stackTrace.toString())
        }
    }


    fun setAudio(audio: Audio) {
        this.audio.set(audio)
    }

    private fun buildMediaSource(filePath: String): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(context, "exoplayer-sample-app")
        val mediaItem = MediaItem.fromUri(filePath)
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    class Factory(private val application: Application, private val soundId: String) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(application, soundId) as T
        }
    }
}