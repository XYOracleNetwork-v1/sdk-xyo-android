package network.xyo.sdk.sample.ui.ble_server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_ble_server.*
import kotlinx.android.synthetic.main.fragment_ble_server.acceptBridging
import kotlinx.android.synthetic.main.fragment_ble_server.autoBridge
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.R
import network.xyo.sdk.sample.ui
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

@kotlin.ExperimentalUnsignedTypes
class BleServerFragment : Fragment() {

    fun addStatus(status: String) {
        ui {
            text_ble_server?.let {
                val sb = StringBuilder()
                sb.append(it.text)
                sb.append("\r\n")
                sb.append(status)
                it.text = sb.toString()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_server, container, false)
    }

    fun updateUI() {
        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
                acceptBridging.setChecked(network.server.acceptBridging)
                acceptBridging.setOnCheckedChangeListener { _, isChecked ->
                    network.server.acceptBridging = isChecked
                }

                autoBridge.setChecked(network.server.autoBridge)
                autoBridge.setOnCheckedChangeListener { _, isChecked ->
                    network.server.autoBridge = isChecked
                }

                listen.setChecked(network.server.listen)
                listen.setOnCheckedChangeListener { _, isChecked ->
                    network.server.listen = isChecked
                }
            }
        }
    }

    override fun onResume() {
        updateUI()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->

                network.server.listener = object : XyoBoundWitnessTarget.Listener() {
                    override fun boundWitnessStarted() {
                        super.boundWitnessStarted()
                        addStatus("Bound Witness Started")
                    }

                    override fun boundWitnessCompleted(boundWitness: XyoBoundWitness?, error:String?) {
                        super.boundWitnessCompleted(boundWitness, error)
                        if (error == null) {
                            addStatus("Bound Witness Completed [${boundWitness?.completed}]")
                        } else {
                            addStatus("Bound Witness Failed [$error]")
                        }
                    }
                }
            }
            updateUI()
            text_ble_server.text = ""
        }
    }
}