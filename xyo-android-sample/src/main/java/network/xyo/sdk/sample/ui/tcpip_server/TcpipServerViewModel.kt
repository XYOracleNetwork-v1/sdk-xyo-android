package network.xyo.sdk.sample.ui.tcpip_client

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TcpipServerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is tcpip server Fragment"
    }
    val text: LiveData<String> = _text
}