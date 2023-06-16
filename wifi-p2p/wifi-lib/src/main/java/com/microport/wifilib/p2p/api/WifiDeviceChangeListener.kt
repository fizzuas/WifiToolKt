package com.microport.wifilib.p2p.api

import android.net.wifi.p2p.WifiP2pDevice

interface WifiDeviceChangeListener {
    fun getPeers(wifiP2pDeviceList: Collection<WifiP2pDevice>?)
}