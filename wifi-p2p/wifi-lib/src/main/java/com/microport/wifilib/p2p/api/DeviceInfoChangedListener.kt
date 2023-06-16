package com.microport.wifilib.p2p.api

import android.net.wifi.p2p.WifiP2pDevice

interface DeviceInfoChangedListener {
    fun onChanged(selfDeviceInfo: WifiP2pDevice?)
}
