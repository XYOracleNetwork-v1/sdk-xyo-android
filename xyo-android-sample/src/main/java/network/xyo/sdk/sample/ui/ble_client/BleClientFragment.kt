package network.xyo.sdk.sample.ui.ble_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_ble_client.*
import network.xyo.sdk.XyoBleClient
import network.xyo.sdk.sample.R
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.ui

@kotlin.ExperimentalUnsignedTypes
class BleClientFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_client, container, false)
    }

    fun addStatus(status: String) {
        ui {
            val sb = StringBuilder()
            sb.append(text_ble_client.text)
            sb.append("\r\n")
            sb.append(status)
            text_ble_client.text = sb.toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->

                network.client.listener = object : XyoBoundWitnessTarget.Listener() {
                    override fun boundWitnessStarted() {
                        super.boundWitnessStarted()
                        addStatus("Bound Witness Started")
                    }

                    override fun boundWitnessCompleted() {
                        super.boundWitnessCompleted()
                        addStatus("Bound Witness Completed")
                    }
                }

                acceptBridging.setChecked(network.client.acceptBridging)
                acceptBridging.setOnCheckedChangeListener { buttonView, isChecked ->
                    network.client.acceptBridging = isChecked
                }

                autoBoundWitness.setChecked(network.client.autoBoundWitness)
                autoBoundWitness.setOnCheckedChangeListener { buttonView, isChecked ->
                    network.client.autoBoundWitness = isChecked
                }

                autoBridge.setChecked(network.client.autoBridge)
                autoBridge.setOnCheckedChangeListener { buttonView, isChecked ->
                    network.client.autoBridge = isChecked
                }

                scan.setChecked(network.client.scan)
                scan.setOnCheckedChangeListener { buttonView, isChecked ->
                    network.client.scan = isChecked
                }
            }
            text_ble_client.text = ""
        }
    }
}