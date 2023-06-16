package com.microport.wifilib.hotspot.api

import android.app.Activity
import android.app.Application

interface IWifiManager {

    fun init(application: Application?)

    fun openWifi() //打开Wifi


    fun openWifiSettingPage(activity: Activity?) //打开wifi设置页面


    fun closeWifi() //关闭wifi


    fun isWifiEnabled(): Boolean //判断wifi是否可用


    fun connect(ssId: String?, pwd: String?, iWifiLogListener: IWifiConnectListener?) //连接wifi


    fun disConnect(ssid: String, listener: IWifiDisConnectListener?) // 断开某个网络

}