package network.xyo.sdk.sample.ui.ble_client

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import kotlinx.android.synthetic.main.fragment_ble_client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.xyo.sdk.sample.R
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.ui

import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness
import java.util.*

@kotlin.ExperimentalUnsignedTypes
class BleClientFragment : Fragment() {

    var deviceCount = 0
    var xyoDeviceCount = 0
    var nearbyXyoDeviceCount = 0

    var autoUpdateUi = false
    var statusText = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_client, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        autoUpdateUi = true
        GlobalScope.launch {
            while(autoUpdateUi) {
                this@BleClientFragment.updateUI()
                delay(900)
            }
        }
    }

    override fun onDetach() {
        autoUpdateUi = false
        super.onDetach()
    }

    fun addStatus(status: String) {
        statusText = "${statusText}\r\n$status"
        text_ble_client?.let {
            ui {
                it.text = statusText
            }
        }
    }

    private fun updateUI() {
        ui {
            if (!(this@BleClientFragment.isDetached)) {
                (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
                    acceptBridging?.isChecked = network.client.acceptBridging
                    acceptBridging?.setOnCheckedChangeListener { _, isChecked ->
                        network.client.acceptBridging = isChecked
                    }

                    autoBoundWitness?.isChecked = network.client.autoBoundWitness
                    autoBoundWitness?.setOnCheckedChangeListener { _, isChecked ->
                        network.client.autoBoundWitness = isChecked
                    }

                    autoBridge?.isChecked = network.client.autoBridge
                    autoBridge?.setOnCheckedChangeListener { _, isChecked ->
                        network.client.autoBridge = isChecked
                    }

                    scan?.isChecked = network.client.scan
                    scan?.setOnCheckedChangeListener { _, isChecked ->
                        network.client.scan = isChecked
                    }

                    deviceCount = network.client.deviceCount
                    xyoDeviceCount = network.client.xyoDeviceCount
                    nearbyXyoDeviceCount = network.client.nearbyXyoDeviceCount
                }
                detected_devices?.text = deviceCount.toString()
                detected_xyo_devices?.text = xyoDeviceCount.toString()
                nearby_xyo_devices?.text = nearbyXyoDeviceCount.toString()
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
                override fun boundWitnessStarted(source: Any?, target: XyoBoundWitnessTarget) {
                    super.boundWitnessStarted(source, target)
                    addStatus("Bound Witness Started [${source?.javaClass?.name}]")
                }

                override fun boundWitnessCompleted(source: Any?, target: XyoBoundWitnessTarget, boundWitness: XyoBoundWitness?, error:String?) {
                    super.boundWitnessCompleted(source, target, boundWitness, error)
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