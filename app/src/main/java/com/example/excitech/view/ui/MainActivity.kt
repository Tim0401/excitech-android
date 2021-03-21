package com.example.excitech.view.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.excitech.R
import com.example.excitech.service.model.Audio

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

    fun list(view: View) {
        val audioListFragment = AudioListFragment()
        supportFragmentManager
            .beginTransaction()
            .addToBackStack("audio")
            .replace(R.id.fragment_container, audioListFragment, null)
            .commit()
    }
}