package network.xyo.sdk.sample.ui.tcpip_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import network.xyo.sdk.sample.R

class TcpipServerFragment : Fragment() {

    private lateinit var notificationsViewModel: TcpipServerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProviders.of(this).get(TcpipServerViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_tcpip_server, container, false)
        val textView: TextView = root.findViewById(R.id.text_tcpip_server)
        notificationsViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }
}