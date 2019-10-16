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
import network.xyo.sdkcorekotlin.boundWitness.XyoBoundWitness

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
        //initializeXyoSimple()
        initializeXyoBleClientOnly()
        //initializeXyoBleServerOnly()
        //initializeXyoBleOnly()
    }

    private fun initializeXyoSimple() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
    }

    private fun initializeXyoBleClientOnly() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
        (node.networks["ble"] as? XyoBleNetwork)?.let { network ->
            network.client.autoBridge = true
            network.client.autoBoundWitness = true
            network.client.scan = true
            network.server.autoBridge = false
            network.server.listen = false
        }
        (node.networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->
            network.client.autoBridge = false
            network.client.autoBoundWitness = false
            network.server.autoBridge = false
            network.server.listen = false
        }
    }

    private fun initializeXyoBleServerOnly() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
        (node.networks["ble"] as? XyoBleNetwork)?.let { network ->
            network.client.autoBridge = false
            network.client.autoBoundWitness = false
            network.client.scan = false
            network.server.autoBridge = true
            network.server.listen = true
        }
        (node.networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->
            network.client.autoBridge = false
            network.client.autoBoundWitness = false
            network.server.autoBridge = false
            network.server.listen = false
        }
    }

    private fun initializeXyoBleOnly() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
        (node.networks["tcpip"] as? XyoTcpIpNetwork)?.let { network ->
            network.client.autoBridge = false
            network.client.autoBoundWitness = false
            network.server.autoBridge = false
            network.server.listen = false
        }
    }
}
