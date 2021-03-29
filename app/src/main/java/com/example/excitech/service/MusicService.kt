package com.example.excitech.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadata.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.Log.DEBUG
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.DefaultEventListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.File


class MusicService : MediaBrowserServiceCompat() {

    //ログ用タグ
    val TAG = MusicService::class.java.simpleName

    //クライアントに返すID onGetRoot / onLoadChildrenで使用
    val ROOT_ID = "root"

    //定期的に処理を回すためのHandler
    var handler: Handler? = null

    lateinit var mSession: MediaSessionCompat
    lateinit var am : AudioManager

    var index = 0 //再生中のインデックス

    lateinit var exoPlayer: SimpleExoPlayer

    //キューに使用するリスト
    var queueItems: MutableList<MediaSessionCompat.QueueItem> = ArrayList()
    // 再生リスト
    var mediaItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Log.d(TAG, "Connected from pkg:$clientPackageName uid:$clientUid")
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        //曲のリストをクライアントに送信
        //今回はROOT_ID以外は無効
        if (parentId == ROOT_ID) {
            result.sendResult(mediaItems)
        } else {
            result.sendResult(ArrayList<MediaBrowserCompat.MediaItem>())
        }
    }

    private fun createMediaItems(files :List<File>) {
        // 再生対象ファイルの読み込み
        // MediaItem配列の作成
        mediaItems = files.map{
            val mediaDescriptionCompat = MediaDescriptionCompat.Builder().apply {
                setTitle(it.name)
                setSubtitle(it.name)
                setMediaId(it.absolutePath)
            }.build()
            MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }.toMutableList()
    }

    private fun createQueue() {
        //キューにアイテムを追加
        queueItems.clear()
        for ((i, media) in mediaItems.withIndex()) {
            queueItems.add(MediaSessionCompat.QueueItem(media.description, i.toLong()))
        }
        //WearやAutoにキューが表示される
        mSession.setQueue(queueItems)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ファイルからMediaItems作成
        val files = applicationContext.filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".3gp") } ?: listOf<File>()
        createMediaItems(files)

        // キューの作成
        createQueue()
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // ファイルからMediaItems作成
        val files = applicationContext.filesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".3gp") } ?: listOf<File>()
        createMediaItems(files)

        //AudioManagerを取得
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        //MediaSessionを初期化
        mSession = MediaSessionCompat(applicationContext, TAG)
        //このMediaSessionが提供する機能を設定
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or  //ヘッドフォン等のボタンを扱う
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or  //キュー系のコマンドの使用をサポート
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS) //再生、停止、スキップ等のコントロールを提供

        //クライアントからの操作に応じるコールバックを設定
        mSession.setCallback(callback)

        //MediaBrowserServiceにSessionTokenを設定
        sessionToken = mSession.sessionToken

        //Media Sessionのメタデータや、プレイヤーのステータスが更新されたタイミングで
        //通知の作成/更新をする
        mSession.controller.registerCallback(object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                createNotification()
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                createNotification()
            }
        })

        // キューの作成
        createQueue()

        //exoPlayerの初期化
        exoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
        //プレイヤーのイベントリスナーを設定
        exoPlayer.addListener(eventListener)
        handler = Handler()
        //500msごとに再生情報を更新
        handler!!.postDelayed(object : Runnable {
            override fun run() {
                //再生中にアップデート
                if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.playWhenReady) updatePlaybackState()

                //再度実行
                handler!!.postDelayed(this, 100)
            }
        }, 100)
    }

    //MediaSession用コールバック
    private val callback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        //曲のIDから再生する
        //WearやAutoのブラウジング画面から曲が選択された場合もここが呼ばれる
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {

            //Uriから再生する
            val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, "AppName"))
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mediaId))

            //今回は簡易的にmediaIdからインデックスを割り出す。
            for (item in queueItems) if (item.description.mediaId == mediaId) index = item.queueId.toInt()
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            mSession.isActive = true
            onPlay()

            //MediaSessionが配信する、再生中の曲の情報を設定
            mSession.setMetadata(MediaMetadataCompat.Builder()
                    .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, mediaId)
                    .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, mediaId)
                    .putString(METADATA_KEY_MEDIA_ID, mediaId)
                    .build())
        }

        //再生をリクエストされたとき
        override fun onPlay() {
            //オーディオフォーカスを要求
            if (am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                //取得できたら再生を始める
                mSession.isActive = true
                exoPlayer.playWhenReady = true
            }
        }

        //一時停止をリクエストされたとき
        override fun onPause() {
            exoPlayer.playWhenReady = false
            //オーディオフォーカスを開放
            am.abandonAudioFocus(afChangeListener)
        }

        //停止をリクエストされたとき
        override fun onStop() {
            onPause()
            mSession.isActive = false
            //オーディオフォーカスを開放
            am.abandonAudioFocus(afChangeListener)
        }

        //シークをリクエストされたとき
        override fun onSeekTo(pos: Long) {
            exoPlayer.seekTo(pos)
        }

        //次の曲をリクエストされたとき
        override fun onSkipToNext() {
            index++
            if (index >= mediaItems.size) //ライブラリの最後まで再生したら
                index = 0 //最初に戻す
            onPlayFromMediaId(queueItems[index].description.mediaId!!, Bundle())
        }

        //前の曲をリクエストされたとき
        override fun onSkipToPrevious() {
            index--
            if (index < 0) //インデックスが0以下になったら
                index = queueItems.size - 1 //最後の曲に移動する
            onPlayFromMediaId(queueItems[index].description.mediaId!!, Bundle())
        }

        //WearやAutoでキュー内のアイテムを選択された際にも呼び出される
        override fun onSkipToQueueItem(i: Long) {
            onPlayFromMediaId(queueItems[i.toInt()].description.mediaId!!, Bundle())
        }

        //Media Button Intentが飛んできた時に呼び出される
        //オーバーライド不要（今回はログを吐くだけ）
        //MediaSessionのplaybackStateのActionフラグに応じてできる操作が変わる
        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val key: KeyEvent? = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            Log.d(TAG, java.lang.String.valueOf(key?.keyCode))
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    //プレイヤーのコールバック
    private val eventListener: Player.EventListener = object : DefaultEventListener() {
        //プレイヤーのステータスが変化した時に呼ばれる
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlaybackState()
        }
    }

    //MediaSessionが配信する、現在のプレイヤーの状態を設定する
    //ここには再生位置の情報も含まれるので定期的に更新する
    private fun updatePlaybackState() {
        var state = PlaybackStateCompat.STATE_NONE
        when (exoPlayer.playbackState) {
            Player.STATE_IDLE -> state = PlaybackStateCompat.STATE_NONE
            Player.STATE_BUFFERING -> state = PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> state = if (exoPlayer.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> {
                state = PlaybackStateCompat.STATE_STOPPED
                mSession.controller.transportControls.skipToNext()
            }
        }

        //プレイヤーの情報、現在の再生位置などを設定する
        //また、MediaButtonIntentでできる操作を設定する
        mSession.setPlaybackState(PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_STOP)
                .setState(state, exoPlayer.currentPosition, exoPlayer.playbackParameters.speed)
                .build())
    }

    //通知を作成、サービスをForegroundにする
    private fun createNotification() {
        val controller = mSession.controller
        val mediaMetadata = controller.metadata
        if (mediaMetadata == null && !mSession.isActive) return
        val description = mediaMetadata.description

        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("my_service", "My Background Service")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        val builder = NotificationCompat.Builder(this, channelId )
        builder //現在の曲の情報を設定
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setSubText(description.description)
                .setLargeIcon(description.iconBitmap) // 通知をクリックしたときのインテントを設定
                //.setContentIntent(createContentIntent()) // 通知がスワイプして消された際のインテントを設定
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP)) // 通知の範囲をpublicにしてロック画面に表示されるようにする
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_input_add) //通知の領域に使う色を設定
                //Androidのバージョンによってスタイルが変わり、色が適用されない場合も多い
                //.setColor(ContextCompat.getColor(this, R.color.colorAccent)) // Media Styleを利用する
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mSession.sessionToken) //通知を小さくたたんだ時に表示されるコントロールのインデックスを設定
                        .setShowActionsInCompactView(1))
                .setOnlyAlertOnce(true)
                .setSound(null)

        // Android4.4以前は通知をスワイプで消せないので
        //キャンセルボタンを表示することで対処
        //今回はminSDKが21なので必要ない
        //.setShowCancelButton(true)
        //.setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
        //        PlaybackStateCompat.ACTION_STOP)));

        //通知のコントロールの設定
        builder.addAction(NotificationCompat.Action(
                R.drawable.ic_media_previous, "prev",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this@MusicService,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))

        //プレイヤーの状態で再生、一時停止のボタンを設定
        if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(NotificationCompat.Action(
                    R.drawable.ic_media_pause, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this@MusicService,
                            PlaybackStateCompat.ACTION_PAUSE)))
        } else {
            builder.addAction(NotificationCompat.Action(
                    R.drawable.ic_media_play, "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this@MusicService,
                            PlaybackStateCompat.ACTION_PLAY)))
        }
        builder.addAction(NotificationCompat.Action(
                R.drawable.ic_media_next, "next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this@MusicService,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))

        startForeground(1, builder.build())

        //再生中以外ではスワイプで通知を消せるようにする
        if (controller.playbackState.state != PlaybackStateCompat.STATE_PLAYING) stopForeground(false)
    }

    //オーディオフォーカスのコールバック
    var afChangeListener = OnAudioFocusChangeListener { focusChange ->
        //フォーカスを完全に失ったら
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            //止める
            mSession.controller.transportControls.pause()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { //一時的なフォーカスロスト
            //止める
            mSession.controller.transportControls.pause()
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) { //通知音とかによるフォーカスロスト（ボリュームを下げて再生し続けるべき）
            //本来なら音量を一時的に下げるべきだが何もしない
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) { //フォーカスを再度得た場合
            //再生
            mSession.controller.transportControls.play()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }
}