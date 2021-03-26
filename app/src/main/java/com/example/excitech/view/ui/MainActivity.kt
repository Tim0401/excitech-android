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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
}