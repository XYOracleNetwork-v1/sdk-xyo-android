package network.xyo.sdk.sample.ui.ble_server

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BleServerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is ble server Fragment"
    }
    val text: LiveData<String> = _text
}