package network.xyo.sdk.sample.ui.ble_server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import network.xyo.sdk.sample.R

class BleServerFragment : Fragment() {

    private lateinit var homeViewModel: BleServerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(BleServerViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_ble_server, container, false)
        val textView: TextView = root.findViewById(R.id.text_ble_server)
        homeViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }
}