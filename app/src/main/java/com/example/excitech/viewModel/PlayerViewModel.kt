package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.annotation.Nullable
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.example.excitech.service.model.Audio
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.MediaItemTransitionReason
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class PlayerViewModel @AssistedInject constructor(application: Application, @Assisted private val audioId: String) : AndroidViewModel(application) {
    // シリアライズ可能
    val audioLiveData: MutableLiveData<Audio> = MutableLiveData()
    val initPlayerLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    private lateinit var player: SimpleExoPlayer

    // 再生する音声が変わったときのイベント
    private val playerEventListener = object : Player.EventListener {
        override fun onMediaItemTransition(
                @Nullable mediaItem: MediaItem?, @MediaItemTransitionReason reason: Int) {
            viewModelScope.launch {
                // duration取得
                val filePath = mediaItem?.mediaId
                var durationMs: Long = 0
                val file = filePath?.let{
                    val file = File(filePath)
                    // 別スレッドで実行 コルーチン
                    durationMs = withContext(Dispatchers.IO) {
                        getDurationMs(filePath)
                    }
                    return@let file
                }

                // 再生しているデータを渡す
                audioLiveData.postValue(
                    Audio(
                        file?.name ?: "",
                        getDurationText(durationMs),
                        SimpleDateFormat(
                            "yyyy/MM/dd HH:mm:ss",
                            Locale.JAPAN
                        ).format(file?.lastModified() ?: 0)
                    )
                )
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    init {
        loadAudio()
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(audioId: String): PlayerViewModel
    }

    companion object {
        fun provideFactory(
                assistedFactory: AssistedFactory,
                audioId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return assistedFactory.create(audioId) as T
            }
        }
    }

    private fun loadAudio() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {
            val files = context.filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".3gp") } ?: listOf<File>()
            val filePath = context.filesDir.path + '/' + audioId
            val file = File(filePath)

            player = SimpleExoPlayer.Builder(context).build().apply {
                playWhenReady = playWhenReady
            }
            // プレイリスト作成
            var findFlg = false
            files.map{
                player.addMediaItem(MediaItem.fromUri(it.absolutePath))
                // 指定ファイルまでスキップする
                if(!findFlg){
                    player.next()
                }
                if(it.absolutePath.equals(filePath)){
                    findFlg = true
                }
            }
            player.addListener(playerEventListener)

            // 別スレッドで実行 コルーチン
            val durationMs = withContext(Dispatchers.IO) {
                getDurationMs(filePath)
            }

            // 再生しているデータを渡す
            audioLiveData.postValue(
                Audio(
                    file.name,
                    getDurationText(durationMs),
                    SimpleDateFormat(
                        "yyyy/MM/dd HH:mm:ss",
                        Locale.JAPAN
                    ).format(file.lastModified())
                )
            )
            // 再生準備
            player.prepare()
            // 再生開始
            player.play()

            initPlayerLiveData.postValue(true)

            // TODO: onPauseのタイミングでplayerのpauseを呼ぶとかする

        } catch (e: Exception) {
            Log.e("loadProject:Failed", e.stackTrace.toString())
        }
    }

    fun setAudio(audio: Audio) {
        this.audioLiveData.value = audio
    }

    fun getPlayer(): SimpleExoPlayer {
        return player
    }

    private fun getDurationText(ms: Long): String {
        val duration = ms / 1000
        val h = duration / 3600
        val m = (duration - h * 3600) / 60
        val s = duration - (h * 3600 + m * 60)
        return  "%02d:%02d:%02d".format(h, m, s)
    }

    private fun getDurationMs(filePath: String): Long {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(filePath)
        return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
    }

}
