package com.microport.wifilib.hotspot.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import android.text.TextUtils
import com.microport.wifilib.hotspot.api.IWifiConnectListener
import com.microport.wifilib.hotspot.api.IWifiDisConnectListener
import com.microport.wifilib.hotspot.api.IWifiManager

class WifiManagerProxy private constructor() : IWifiManager {
    private object SingletonHolder {
        val INSTANCE = WifiManagerProxy()
    }

    companion object {
        val instance = SingletonHolder.INSTANCE
    }

    private var manager: WifiManager? = null
    private val mConnector = WifiConnector()

    override fun init(application: Application?) {
        requireNotNull(application) { "Application cant be null!" }
        if (manager == null) {
            manager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            mConnector.init(manager)
        }
    }

    override fun openWifi() {
        checkInit()
        if (!isWifiEnabled()) {
            manager!!.isWifiEnabled = true
        }
    }

    override fun openWifiSettingPage(activity: Activity?) {
        checkInit()
        if (activity == null) {
            return
        }
        activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    override fun closeWifi() {
        checkInit()
        if (isWifiEnabled()) {
            manager!!.isWifiEnabled = false
        }
    }

    override fun isWifiEnabled(): Boolean {
        checkInit()
        return manager!!.isWifiEnabled
    }

    override fun connect(ssId: String?, pwd: String?, iWifiLogListener: IWifiConnectListener?) {
        checkInit()
        requireNotNull(iWifiLogListener) { " IWifiConnectListener cant be null !" }
        mConnector.connect(ssId!!, pwd!!, WifiConnector.WifiCipherType.WIFI_CIPHER_WPA, iWifiLogListener)
    }

    override fun disConnect(ssid: String, listener: IWifiDisConnectListener?) {
        checkInit()
        requireNotNull(listener) { " IWifiDisConnectListener cant be null !" }
        if (TextUtils.isEmpty(ssid)) {
            listener.onDisConnectFail(" WIFI名称不能为空! ")
            return
        }
        val newSSID = "\"" + ssid + "\""
        val wifiInfo = manager!!.connectionInfo
        if (wifiInfo != null && !TextUtils.isEmpty(newSSID) && TextUtils.equals(newSSID, wifiInfo.ssid)) {
            val netId = wifiInfo.networkId
            manager!!.disableNetwork(netId)
            listener.onDisConnectSuccess()
        } else {
            listener.onDisConnectFail(" wifi状态异常 或者 此时就没有连接上对应的WIFI ！ ")
        }
    }

    private fun checkInit() {
        requireNotNull(manager) { "You must call init()  before other methods!" }
    }

}