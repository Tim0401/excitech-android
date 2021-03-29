package com.example.excitech.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.session.PlaybackState
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.excitech.service.MusicService
import com.example.excitech.service.model.Audio
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
    val currentTimeLiveData: MutableLiveData<String> = MutableLiveData("00:00:00")
    val currentTimeMsLiveData: MutableLiveData<Int> = MutableLiveData(0)
    val isPlayingLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    private val filePath = context.filesDir.path + '/' + audioId

    private lateinit var mBrowser: MediaBrowserCompat
    private var mController: MediaControllerCompat? = null

    init {
        initAudio()
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

    private fun initAudio() {
        try {
            //MediaBrowserを初期化
            mBrowser = MediaBrowserCompat(context, ComponentName(context, MusicService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    try {
                        //接続が完了するとSessionTokenが取得できるので
                        //それを利用してMediaControllerを作成
                        mController = MediaControllerCompat(context, mBrowser.sessionToken)

                        mController?.let {
                            //サービスから送られてくるプレイヤーの状態や曲の情報が変更された際のコールバックを設定
                            it.registerCallback(controllerCallback)

                            //既に再生中だった場合コールバックを自ら呼び出してUIを更新
                            if (it.playbackState != null && it.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                                controllerCallback.onMetadataChanged(it.metadata)
                                controllerCallback.onPlaybackStateChanged(it.playbackState)
                            }
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

    fun loadAudio() = viewModelScope.launch {
        //サービスは開始しておく
        //Activity破棄と同時にServiceも停止して良いならこれは不要
        context.startService(Intent(context, MusicService::class.java))
    }

    fun startOrStopPlaying(){
        if(isPlayingLiveData.value == true){
            mController?.transportControls?.pause()
            isPlayingLiveData.postValue(false)
        } else {
            mController?.transportControls?.play()
            isPlayingLiveData.postValue(true)
        }
    }

    fun unsubscribe() {
        mController?.unregisterCallback(controllerCallback)
    }

    fun subscribe() {
        mController?.registerCallback(controllerCallback)
    }

    private fun play(id: String) {
        //MediaControllerからサービスへ操作を要求するためのTransportControlを取得する
        //playFromMediaIdを呼び出すと、サービス側のMediaSessionのコールバック内のonPlayFromMediaIdが呼ばれる
        mController?.transportControls?.playFromMediaId(id, null)
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
                metadata.description.mediaId?.let {
                    val file = File(it)
                    // 別スレッドで実行 コルーチン
                    val durationMs = withContext(Dispatchers.IO) {
                        getDurationMs(it)
                    }
                    // 再生しているデータを渡す
                    audioLiveData.postValue(
                            Audio(
                                    file.name,
                                    file.absolutePath,
                                    durationMs,
                                    getDurationText(durationMs),
                                    SimpleDateFormat(
                                            "yyyy/MM/dd HH:mm:ss",
                                            Locale.JAPAN
                                    ).format(file.lastModified())
                            )
                    )
                }
            }
        }

        //プレイヤーの状態が変更された時に呼び出される
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            currentTimeLiveData.postValue(getDurationText(state.position))
            currentTimeMsLiveData.postValue(state.position.toInt())
            isPlayingLiveData.postValue(state.state == PlaybackState.STATE_PLAYING)
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
