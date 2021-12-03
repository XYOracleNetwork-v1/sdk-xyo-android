package network.xyo.sdk.sample.ui.ble_server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.InternalCoroutinesApi
import network.xyo.sdk.XyoBleNetwork
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.sample.databinding.FragmentBleServerBinding
import network.xyo.sdk.sample.ui
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

@InternalCoroutinesApi
@kotlin.ExperimentalUnsignedTypes
class BleServerFragment : Fragment() {

    var statusText = ""
    private var _binding: FragmentBleServerBinding? = null
    private val binding get() = _binding!!

    fun addStatus(status: String) {
        statusText = "${statusText}\r\n$status"
        binding.textBleServer.let {
            ui {
                it.text = statusText
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBleServerBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    fun updateUI() {
        ui {
            (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
                binding.acceptBridging.isChecked = network.server.acceptBridging
                binding.acceptBridging.setOnCheckedChangeListener { _, isChecked ->
                    network.server.acceptBridging = isChecked
                }

                binding.autoBridge.isChecked = network.server.autoBridge
                binding.autoBridge.setOnCheckedChangeListener { _, isChecked ->
                    network.server.autoBridge = isChecked
                }

                binding.listen.isChecked = network.server.listen
                binding.listen.setOnCheckedChangeListener { _, isChecked ->
                    network.server.listen = isChecked
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
        _binding = null
        (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.server?.listeners?.remove("sample")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (XyoSdk.nodes[0].networks["ble"] as? XyoBleNetwork)?.let { network ->
            ui {
                network.server.listeners["sample"] = object : XyoBoundWitnessTarget.Listener() {
                    override fun boundWitnessStarted(source: Any?, target: XyoBoundWitnessTarget) {
                        super.boundWitnessStarted(source, target)
                        addStatus("Bound Witness Started [${source?.javaClass?.name}]")
                    }

                    override fun boundWitnessCompleted(source: Any?, target: XyoBoundWitnessTarget, boundWitness: XyoBoundWitness?, error:String?) {
                        super.boundWitnessCompleted(source, target, boundWitness, error)
                        val index = target.relayNode.originState.index.valueCopy.toList().toString()
                        if (error == null) {
                            addStatus("Bound Witness Completed [$index] [${boundWitness?.completed}]")
                        } else {
                            addStatus("Bound Witness Failed [$error]")
                        }
                        addStatus("- - - - - -")
                    }
                }
                updateUI()
                binding.textBleServer.text = ""
                binding.publicKey.text = network.server.publicKey
            }
        }
    }
}