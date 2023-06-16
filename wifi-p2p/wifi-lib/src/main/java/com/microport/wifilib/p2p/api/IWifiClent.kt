package com.microport.wifilib.p2p.api

import android.net.wifi.p2p.WifiP2pDevice
import com.microport.wifilib.p2p.param.DataFrame

interface IWifiClient {
    fun startDiscovery(searchCallback: SearchCallback?)
    fun stopDiscovery()
    fun connect(device: WifiP2pDevice)
    fun disConnect(callback: DisConnectCallback)
    fun send(dataFrame: DataFrame)

}

interface SearchCallback {
    fun onStart()
    fun onSuccess()
    fun onFailure(error: Error)
}


interface DisConnectCallback {
    fun onSuccess()
    fun onFail(error: Error)
}




