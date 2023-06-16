package com.microport.wifilib.hotspot.api

interface IWifiConnectListener {
    //开始连接
    fun onConnectStart()

    // 连接成功
    fun onConnectSuccess(selfIP:String,serverIP:String)

    //连接失败
    fun onConnectFail(errorMsg: String?)
}