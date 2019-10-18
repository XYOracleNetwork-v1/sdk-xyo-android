[logo]: https://cdn.xy.company/img/brand/XY_Logo_GitHub.png

![logo]

# sdk-xyo-android

[![](https://travis-ci.org/XYOracleNetwork/sdk-core-kotlin.svg?branch=master)](https://travis-ci.org/XYOracleNetwork/sdk-xyo-android)[![Codacy Badge](https://api.codacy.com/project/badge/Grade/2fb2eb69c1db455299ffce57b0216aa6)](https://www.codacy.com/app/XYOracleNetwork/sdk-xyo-android?utm_source=github.com&utm_medium=referral&utm_content=XYOracleNetwork/sdk-xyo-android&utm_campaign=Badge_Grade) [![Maintainability](https://api.codeclimate.com/v1/badges/af641257b27ecea22a9f/maintainability)](https://codeclimate.com/github/XYOracleNetwork/sdk-xyo-android/maintainability) [![](https://img.shields.io/gitter/room/XYOracleNetwork/Stardust.svg)](https://gitter.im/XYOracleNetwork/Dev)

## Table of Contents

-   [Title](#sdk-xyo-android)
-   [Description](#description)
-   [Usage](#usage)
-   [Architecture](#architecture)
-   [Maintainers](#maintainers)
-   [Contributing](#contributing)
-   [License](#license)
-   [Credits](#credits)

## Description 

A high-level SDK for interacting with the XYO network.
Including BLE, TCP/IP, Bound Witnessing, and Bridging. 

## Start Here 

Copy this code to test. Look below for specific usage. 

``` kotlin 
  // callback for node events
          val listener = object : XyoBoundWitnessTarget.Listener() {
              override fun boundWitnessCompleted(boundWitness: XyoBoundWitness?, error: String?) {
                  super.boundWitnessCompleted(boundWitness, error)

                  println("New bound witness!")
              }

              override fun boundWitnessStarted() {
                  super.boundWitnessStarted()

                  println("Bound witness started!")

              }
          }
          

          // build and configure the node
          val builder = XyoNodeBuilder()
          builder.setListener(listener)

          // create the node
          val context = getContextSomehow()
          val node = builder.build(context)

          // configure tcp
          val tcpNetwork = node.networks["tcp"] ?: return
          tcpNetwork.client.autoBridge = true
          tcpNetwork.client.autoBoundWitness = true
          tcpNetwork.client.scan = false

          // configure ble
          val bleNetwork = node.networks["ble"] ?: return
          bleNetwork.client.autoBridge = true
          bleNetwork.client.autoBoundWitness = true
          bleNetwork.client.scan = false
```

## Usage

Build an XYO Node 

```kotlin
val builder = XYONodeBuilder()
``` 

After calling the node builder, you can start the build

```kotlin
val node = XyoNode()

node = builder.build(this)
```

Once you have a build, you have access to properties to help you shape your node and what you want out of it. 

```kotlin
node.networks["this can be "ble" or "tcpip""]
```

After choosing the network, you have these properties available

Client

```kotlin
val network = node.networks["network"]

network.client.autoBridge
network.client.autoBoundWitness
network.client.scan
```

Server

```kotlin
val network = node.networks["network"]

network.server.autoBridge
network.server.listen
```

These will allow your app to actively seek devices to bound witness with and bridge from the client to the server.

You can also get payload data from the bound witness 

```kotlin
node.listener.getPayloadData(target: boundWitnessTarget)
```

This will return a byteArray.

There are other properties from the client and server which you can find in the source code as well as a reference guide that we have prepared. 


## Architecture

This sdk is built on a client/server to ensure ease of understanding during development. (The client takes on "central" role, and the server the "peripheral"). This allows us to define roles with simplicity. 

> SDK-XYO-ANDROID TREE

- XyoSDK
  - mutableList `<XyoNode>` 
    - `XyoNode(storage, networks)`
      - `listeners`
        - `boundWitnessTarget`
    - XyoClient, XyoServer
      - Ble
        - `context`
        - `relayNode`
        - `procedureCatalog`
        - `autoBridge`
        - `acceptBridging`
        - `autoBoundWitness`
        - `scan`
    
      - TcpIp
        - `relayNode`
        - `procedureCatalog`
        - `autoBridge`
        - `acceptBridging`
        - `autoBoundWitness`

## Sample App

Please refer to the [xyo-android-sample](/xyo-android-sample/src/main/java/network/xyo/sdk/sample/MainActivity.kt) for an exmple implementation for bound witness and bridging. 

### Install

To use the sample app to measure functionality

- Launch [Android Studio](https://developer.android.com/studio/install)
- Click on `Open an existing Android Studio Project`
- Navigate to `<path to the sdk-xyo-android>/xyo-android-sample` in your file explorer

Once you open the sample in Android Studio it will go through the build process.

You can then run the app in a simulator of your choice or an Android device. 

This sample app includes client bridging and bound witnessing with a BLE server listener. 

## Maintainers

- Arie Trouw

## Contributing

## License

See the [LICENSE](LICENSE) file for license details.

## Credits

Made with 🔥and ❄️ by [XY - The Persistent Company](https://www.xy.company)
