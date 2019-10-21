package network.xyo.sdk.sample.ui.ble_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import kotlinx.android.synthetic.main.fragment_ble_client.*
import network.xyo.sdk.sample.R
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.ui

import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

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
            text_ble_client?.let {
                val sb = StringBuilder()
                sb.append(it.text)
                sb.append("\r\n")
                sb.append(status)
                it.text = sb.toString()
            }
        }
    }

    fun updateUI() {
        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
                acceptBridging.isChecked = network.client.acceptBridging
                acceptBridging.setOnCheckedChangeListener { _, isChecked ->
                    network.client.acceptBridging = isChecked
                }

                autoBoundWitness.isChecked = network.client.autoBoundWitness
                autoBoundWitness.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBoundWitness = isChecked
                }

                autoBridge.isChecked = network.client.autoBridge
                autoBridge.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBridge = isChecked
                }

                scan.isChecked = network.client.scan
                scan.setOnCheckedChangeListener { _, isChecked ->
                    network.client.scan = isChecked
                }
            }
        }
    }

    override fun onResume() {
        updateUI()
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.client?.listeners?.remove("sample")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->

            network.client.listeners["sample"] = object : XyoBoundWitnessTarget.Listener() {
                override fun boundWitnessStarted(target: XyoBoundWitnessTarget) {
                    super.boundWitnessStarted(target)
                    addStatus("Bound Witness Started")
                }

                override fun boundWitnessCompleted(target: XyoBoundWitnessTarget, boundWitness: XyoBoundWitness?, error:String?) {
                    super.boundWitnessCompleted(target, boundWitness, error)
                    val index = target.relayNode.originState.index.valueCopy.toList().toString()
                    if (error == null) {
                        addStatus("Bound Witness Completed $index [${boundWitness?.completed}]")
                    } else {
                        addStatus("Bound Witness Failed [$error]")
                    }
                    addStatus("- - - - - -")
                }

            }

            ui {
                text_ble_client.text = ""
                publicKey.text = network.client.publicKey
            }
        }
    }
}