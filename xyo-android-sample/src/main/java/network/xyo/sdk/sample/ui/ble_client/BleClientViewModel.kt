package network.xyo.sdk.sample.ui.ble_client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BleClientViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is ble client Fragment"
    }
    val text: LiveData<String> = _text
}