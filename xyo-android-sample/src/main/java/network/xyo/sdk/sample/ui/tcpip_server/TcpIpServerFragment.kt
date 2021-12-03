package network.xyo.sdk.sample.ui.tcpip_server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import network.xyo.sdk.sample.databinding.FragmentTcpipServerBinding
import network.xyo.sdk.sample.ui

class TcpIpServerFragment : Fragment() {

    private var _binding: FragmentTcpipServerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTcpipServerBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addStatus("Not Implemented for Android")
    }

    fun addStatus(status: String) {
        ui {
            binding.textTcpipServer.let {
                val sb = StringBuilder()
                sb.append(it.text)
                sb.append("\r\n")
                sb.append(status)
                it.text = sb.toString()
            }
        }
    }
}