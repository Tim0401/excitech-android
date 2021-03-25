package com.example.excitech.view.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.excitech.R
import com.example.excitech.service.MusicService
import com.example.excitech.service.model.Audio
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var mBrowser: MediaBrowserCompat
    lateinit var mController: MediaControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //サービスは開始しておく
        //Activity破棄と同時にServiceも停止して良いならこれは不要
        startService(Intent(this, MusicService::class.java))

        //MediaBrowserを初期化
        mBrowser = MediaBrowserCompat(this, ComponentName(this, MusicService::class.java), connectionCallback, null)
        //接続(サービスをバインド)
        mBrowser.connect()

        if (savedInstanceState == null) {
            val fragment = RecordFragment() //一覧のFragment
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment, TAG_OF_RECORD_FRAGMENT)
                .commit()
        }
    }

    fun show(audio: Audio) {
        val projectFragment = PlayerFragment.forAudio(audio.name) //詳細のFragment
        supportFragmentManager
            .beginTransaction()
            .addToBackStack("audio")
            .replace(R.id.fragment_container, projectFragment, null)
            .commit()
    }

    fun list(@Suppress("UNUSED_PARAMETER") view: View) {
        val audioListFragment = AudioListFragment()
        supportFragmentManager
            .beginTransaction()
            .addToBackStack("audio")
            .replace(R.id.fragment_container, audioListFragment, null)
            .commit()
    }

    //接続時に呼び出されるコールバック
    private val connectionCallback: MediaBrowserCompat.ConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            try {
                //接続が完了するとSessionTokenが取得できるので
                //それを利用してMediaControllerを作成
                mController = MediaControllerCompat(this@MainActivity, mBrowser.sessionToken)
                //サービスから送られてくるプレイヤーの状態や曲の情報が変更された際のコールバックを設定
                mController.registerCallback(controllerCallback)

                //既に再生中だった場合コールバックを自ら呼び出してUIを更新
                if (mController.playbackState != null && mController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    controllerCallback.onMetadataChanged(mController.metadata)
                    controllerCallback.onPlaybackStateChanged(mController.playbackState)
                }
            } catch (ex: RemoteException) {
                ex.printStackTrace()
                Toast.makeText(this@MainActivity, ex.message, Toast.LENGTH_LONG).show()
            }
            //サービスから再生可能な曲のリストを取得
            mBrowser.subscribe(mBrowser.root, subscriptionCallback)
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
            if (mController.playbackState == null && children.isNotEmpty()) children[0].mediaId?.let { play(it) }
        }
    }

    //MediaControllerのコールバック
    private val controllerCallback: MediaControllerCompat.Callback = object : MediaControllerCompat.Callback() {
        //再生中の曲の情報が変更された際に呼び出される
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            // TODO: change UI
        }

        //プレイヤーの状態が変更された時に呼び出される
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // TODO: change UI
        }
    }
}