package com.itsabugnotafeature.fitocrazy.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NotificationsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Profile Fragment under construction"
    }
    val text: LiveData<String> = _text
}