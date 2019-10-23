package network.xyo.sdk.sample

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import network.xyo.sdk.*
import network.xyo.sdkcorekotlin.heuristics.XyoHeuristicGetter
import network.xyo.sdkobjectmodelkotlin.structure.XyoObjectStructure
import java.nio.ByteBuffer
import network.xyo.sdkcorekotlin.schemas.*
import network.xyo.sdkcorekotlin.heuristics.*
import network.xyo.sdkobjectmodelkotlin.structure.XyoIterableStructure

@kotlin.ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    lateinit var node: XyoNode
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            initializeXyoSimpleWithGps()
            //initializeXyoSimple()
            //initializeXyoBleClientOnly()
            //initializeXyoBleServerOnly()
            //initializeXyoBleOnly()
            ui {
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
            }
        }
    }

    private suspend fun initializeXyoSimple() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
    }

    private suspend fun initializeXyoSimpleWithGps() {
        val builder = XyoNodeBuilder()
        node = builder.build(this)
        (node.networks["ble"] as? XyoBleNetwork)?.client?.relayNode?.addHeuristic(
            "GPS",
            object: XyoHeuristicGetter {
                override fun getHeuristic(): XyoObjectStructure? {
                    val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                        if (lastLocation != null) {
                            val encodedLat = ByteBuffer.allocate(8).putDouble(lastLocation.latitude).array()
                            val encodedLng = ByteBuffer.allocate(8).putDouble(lastLocation.longitude).array()
                            val lat = XyoObjectStructure.newInstance(XyoSchemas.LAT, encodedLat)
                            val lng = XyoObjectStructure.newInstance(XyoSchemas.LNG, encodedLng)

                            return XyoIterableStructure.createUntypedIterableObject(XyoSchemas.GPS, arrayOf(lat, lng))
                        }
                    }
                    return null
                }
            }
        )
    }

    private suspend fun initializeXyoBleClientOnly() {
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

    private suspend fun initializeXyoBleServerOnly() {
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

    private suspend fun initializeXyoBleOnly() {
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
