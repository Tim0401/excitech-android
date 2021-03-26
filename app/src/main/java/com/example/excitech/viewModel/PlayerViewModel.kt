package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.example.excitech.service.MusicService
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

    private lateinit var mBrowser: MediaBrowserCompat
    private lateinit var mController: MediaControllerCompat

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val filePath = context.filesDir.path + '/' + audioId

    init {
        // loadAudio()
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

    fun loadAudio() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {
            //サービスは開始しておく
            //Activity破棄と同時にServiceも停止して良いならこれは不要
            context.stopService(Intent(context, MusicService::class.java))
            context.startService(Intent(context, MusicService::class.java))

            //MediaBrowserを初期化
            mBrowser = MediaBrowserCompat(context, ComponentName(context.applicationContext, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    try {
                        //接続が完了するとSessionTokenが取得できるので
                        //それを利用してMediaControllerを作成
                        mController = MediaControllerCompat(context, mBrowser.sessionToken)
                        //サービスから送られてくるプレイヤーの状態や曲の情報が変更された際のコールバックを設定
                        mController.registerCallback(controllerCallback)

                        //既に再生中だった場合コールバックを自ら呼び出してUIを更新
                        if (mController.playbackState != null && mController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                            controllerCallback.onMetadataChanged(mController.metadata)
                            controllerCallback.onPlaybackStateChanged(mController.playbackState)
                        }
                    } catch (ex: RemoteException) {
                        ex.printStackTrace()
                        Toast.makeText(context, ex.message, Toast.LENGTH_LONG).show()
                    }
                    //サービスから再生可能な曲のリストを取得
                    mBrowser.subscribe(mBrowser.root, subscriptionCallback)
                } }, null)
            //接続(サービスをバインド)
            mBrowser.connect()

        } catch (e: Exception) {
            Log.e("loadProject:Failed", e.stackTrace.toString())
        }
    }

    private fun play(id: String) {
        //MediaControllerからサービスへ操作を要求するためのTransportControlを取得する
        //playFromMediaIdを呼び出すと、サービス側のMediaSessionのコールバック内のonPlayFromMediaIdが呼ばれる
        mController.transportControls.playFromMediaId(id, null)
    }

    //Subscribeした際に呼び出されるコールバック
    private val subscriptionCallback: MediaBrowserCompat.SubscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            //既に再生中でなければ初めの曲を再生をリクエスト
            // if (mController.playbackState == null && children.isNotEmpty()) { play(filePath) }
            play(filePath)
        }
    }

    //MediaControllerのコールバック
    private val controllerCallback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        //再生中の曲の情報が変更された際に呼び出される
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            // change UI
            viewModelScope.launch {
                val file = File(filePath)
                // 別スレッドで実行 コルーチン
                val durationMs = withContext(Dispatchers.IO) {
                    getDurationMs(filePath)
                }
                // 再生しているデータを渡す
                audioLiveData.postValue(
                        Audio(
                                file.name,
                                file.absolutePath,
                                getDurationText(durationMs),
                                SimpleDateFormat(
                                        "yyyy/MM/dd HH:mm:ss",
                                        Locale.JAPAN
                                ).format(file.lastModified())
                        )
                )
            }
        }

        //プレイヤーの状態が変更された時に呼び出される
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // TODO: change UI
        }
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
