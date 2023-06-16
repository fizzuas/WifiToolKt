package com.microport.wifilib.p2p.api

import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo

interface IWifiServer {
    fun createGroup(callBack: CreateGroupCallBack)
    fun removeGroup(callBack: RemoveGroupCallBack)
}


interface CreateGroupCallBack {
    fun onSuccess(wifiP2pInfo: WifiP2pInfo?)
    fun onError(error: Error)
}

interface RemoveGroupCallBack {
    fun onSuccess()
    fun onError(error: Error)
}

interface GroupInfoChangedListener{
    fun onChanged(wifiP2pInfo: WifiP2pInfo?,groupInfo: WifiP2pGroup?)
}




