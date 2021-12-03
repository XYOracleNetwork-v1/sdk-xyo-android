package network.xyo.sdk.sample.ui.tcpip_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import network.xyo.sdk.XyoBoundWitnessTarget
import network.xyo.sdk.XyoSdk
import network.xyo.sdk.XyoTcpIpNetwork
import network.xyo.sdk.sample.databinding.FragmentTcpipClientBinding
import network.xyo.sdk.sample.ui
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

@kotlin.ExperimentalUnsignedTypes
class TcpIpClientFragment : Fragment() {

    private var _binding: FragmentTcpipClientBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTcpipClientBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    fun addStatus(status: String) {
        ui {
            val sb = StringBuilder()
            sb.append(binding.textTcpipClient.text)
            sb.append("\r\n")
            sb.append(status)
            binding.textTcpipClient.text = sb.toString()
        }
    }

    fun updateUI() {
        ui {
            (XyoSdk.nodes[0].networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->
                binding.acceptBridging.isChecked = network.client.acceptBridging
                binding.acceptBridging.setOnCheckedChangeListener { _, isChecked ->
                    network.client.acceptBridging = isChecked
                }

                binding.autoBoundWitness.isChecked = network.client.autoBoundWitness
                binding.autoBoundWitness.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBoundWitness = isChecked
                }

                binding.autoBridge.isChecked = network.client.autoBridge
                binding.autoBridge.setOnCheckedChangeListener { _, isChecked ->
                    network.client.autoBridge = isChecked
                }

                binding.scan.isChecked = network.client.scan
                binding.scan.setOnCheckedChangeListener { _, isChecked ->
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
        _binding = null
        (XyoSdk.nodes[0].networks["tcpip"] as? XyoTcpIpNetwork)?.client?.listeners?.remove("sample")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (XyoSdk.nodes[0].networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->

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
                binding.textTcpipClient.text = ""
                binding.publicKey.text = network.client.publicKey
            }
        }
    }
}