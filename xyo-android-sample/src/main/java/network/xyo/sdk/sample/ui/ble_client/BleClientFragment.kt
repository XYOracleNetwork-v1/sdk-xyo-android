package network.xyo.sdk.sample.ui.ble_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import network.xyo.sdk.sample.R

class BleClientFragment : Fragment() {

    private lateinit var bleClientViewModel: BleClientViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bleClientViewModel =
            ViewModelProviders.of(this).get(BleClientViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_ble_client, container, false)
        val textView: TextView = root.findViewById(R.id.text_ble_client)
        bleClientViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }
}