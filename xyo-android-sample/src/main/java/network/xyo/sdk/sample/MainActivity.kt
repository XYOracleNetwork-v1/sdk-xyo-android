package network.xyo.sdk.sample

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import network.xyo.ble.generic.devices.XYBluetoothDevice
import network.xyo.ble.generic.scanner.XYSmartScan
import network.xyo.sdk.*
import network.xyo.base.XYBase

@kotlin.ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    lateinit var node: XyoNode
    val log = XYBase.log("Xyo-MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_ble_client, R.id.navigation_ble_server, R.id.navigation_tcpip_client, R.id.navigation_tcpip_server
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        initializeXyo()
    }

    private fun initializeXyo() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
        (node.networks["ble"] as? XyoBleNetwork)?.let { network ->
            network.client.autoBridge = true
            network.client.autoBoundWitness = true
            network.server.autoBridge = true
            network.server.listen = true
            network.client.scanner.addListener("sample", object: XYSmartScan.Listener() {
                override fun entered(device: XYBluetoothDevice) {
                    //log.info("Device Entered: $device")
                    super.entered(device)
                }
                override fun exited(device: XYBluetoothDevice) {
                    //log.info("Device Exited: $device")
                    super.exited(device)
                }
                override fun detected(device: XYBluetoothDevice) {
                    //log.info("Device Detected: $device")
                    super.detected(device)
                }
            })
            network.client.scan = true
            if (network.client.scanner.status != XYSmartScan.Status.Enabled) {
                log.error("Scanner Error: ${network.client.scanner.status}", false)
            }
        }
        (node.networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->
            network.client.autoBridge = true
            network.client.autoBoundWitness = true
            network.server.autoBridge = true
            network.server.listen = true
        }

        node.setAllListeners(object: XyoBoundWitnessTarget.Listener() {
            override fun boundWitnessStarted() {
                log.info("BoundWitness Started")
                super.boundWitnessStarted()
            }

            override fun boundWitnessCompleted() {
                log.info("BoundWitness Completed")
                super.boundWitnessCompleted()
            }

            override fun getPayloadData(): ByteArray {
                return byteArrayOf(0xff.toByte(), 0xff.toByte())
            }
        })
    }
}
