package network.xyo.sdk.sample.ui.ble_client

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.xyo.sdk.sample.R
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.databinding.FragmentBleClientBinding
import network.xyo.sdk.sample.ui

import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

@InternalCoroutinesApi
@kotlin.ExperimentalUnsignedTypes
class BleClientFragment : Fragment() {

    var deviceCount = 0
    var xyoDeviceCount = 0
    var nearbyXyoDeviceCount = 0

    var autoUpdateUi = false
    var statusText = ""

    private var _binding: FragmentBleClientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBleClientBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.client?.listeners?.remove("sample")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        autoUpdateUi = true
        GlobalScope.launch {
            while(autoUpdateUi) {
                this@BleClientFragment.updateUI()
                delay(1000)
            }
        }
    }

    override fun onDetach() {
        autoUpdateUi = false
        super.onDetach()
    }

    fun addStatus(status: String) {
        statusText = "${statusText}\r\n$status"
        ui {
            binding.textBleClient.text = statusText
        }
    }

    @InternalCoroutinesApi
    private fun updateUI() {
        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
                binding.acceptBridging.isChecked = network.client.acceptBridging
                binding.acceptBridging?.setOnCheckedChangeListener { _, isChecked ->
                    network.client.acceptBridging = isChecked
                }

                binding.autoBoundWitness?.isChecked = network.client.autoBoundWitness
                binding.autoBoundWitness?.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBoundWitness = isChecked
                }

                binding.autoBridge?.isChecked = network.client.autoBridge
                binding.autoBridge?.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBridge = isChecked
                }

                binding.scan?.isChecked = network.client.scan
                binding.scan?.setOnCheckedChangeListener { _, isChecked ->
                    network.client.scan = isChecked
                }

                deviceCount = network.client.deviceCount
                xyoDeviceCount = network.client.xyoDeviceCount
                nearbyXyoDeviceCount = network.client.nearbyXyoDeviceCount
            }
            binding.detectedDevices?.text = deviceCount.toString()
            binding.detectedXyoDevices?.text = xyoDeviceCount.toString()
            binding.nearbyXyoDevices?.text = nearbyXyoDeviceCount.toString()
        }
    }

    override fun onResume() {
        updateUI()
        super.onResume()
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
                binding.textBleClient.text = ""
                binding.publicKey.text = network.client.publicKey
            }
        }
    }
}