package com.microport.wifilib.hotspot.api

interface IWifiDisConnectListener {
    //断开成功
    fun onDisConnectSuccess()

    //断开失败
    fun onDisConnectFail(errorMsg: String?)

}