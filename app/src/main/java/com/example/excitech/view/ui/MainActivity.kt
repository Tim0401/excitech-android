package com.example.excitech.view.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.excitech.R

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
}