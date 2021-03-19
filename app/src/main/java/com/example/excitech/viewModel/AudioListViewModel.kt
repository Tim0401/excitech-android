package com.example.excitech.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.excitech.service.model.Audio
import kotlinx.coroutines.launch
import java.lang.Exception

class AudioListViewModel(application: Application) : AndroidViewModel(application) {
    var audioListLiveData: MutableLiveData<List<Audio>> = MutableLiveData()

    init {
        loadAudioList()
    }

    private fun loadAudioList() = viewModelScope.launch { //onCleared() のタイミングでキャンセルされる
        try {
            audioListLiveData.postValue(List<Audio>(1) { Audio("aaa") })
        } catch (e: Exception){
            e.stackTrace
        }
    }
}