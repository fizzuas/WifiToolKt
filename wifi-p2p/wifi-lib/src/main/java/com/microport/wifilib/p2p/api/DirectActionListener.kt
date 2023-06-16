package com.microport.wifilib.p2p.api

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.ChannelListener

interface DirectActionListener : ChannelListener {
    fun wifiP2pEnabled(enabled: Boolean)
    fun onConnected(wifiP2pInfo: WifiP2pInfo?,groupInfo: WifiP2pGroup?)
    fun onDisconnected(wifiP2pInfo: WifiP2pInfo?,groupInfo: WifiP2pGroup?)
    fun onSelfDeviceChanged(wifiP2pDevice: WifiP2pDevice?)
    fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice?>?)
}


