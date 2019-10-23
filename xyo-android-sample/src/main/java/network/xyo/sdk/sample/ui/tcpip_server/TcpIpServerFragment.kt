package network.xyo.sdk.sample.ui.tcpip_server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_tcpip_server.*
import network.xyo.sdk.sample.R
import network.xyo.sdk.sample.ui

class TcpIpServerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tcpip_server, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addStatus("Not Implemented for Android")
    }

    fun addStatus(status: String) {
        ui {
            text_tcpip_server?.let {
                val sb = StringBuilder()
                sb.append(it.text)
                sb.append("\r\n")
                sb.append(status)
                it.text = sb.toString()
            }
        }
    }
}