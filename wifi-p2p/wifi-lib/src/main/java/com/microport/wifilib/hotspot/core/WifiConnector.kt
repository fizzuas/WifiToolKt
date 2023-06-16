package com.microport.wifilib.hotspot.core

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
import com.microport.wifilib.hotspot.api.IWifiConnectListener
import timber.log.Timber
import java.math.BigInteger
import java.net.InetAddress


class WifiConnector {
    private val tag = "WifiConnector"
    private var wifiManager: WifiManager? = null
    private var iWifiConnectListener: IWifiConnectListener? = null

    //WIFI_CIPHER_WEP是WEP ，WIFI_CIPHER_WPA是WPA，WIFI_CIPHER_NO_PASS没有密码
    enum class WifiCipherType {
        WIFI_CIPHER_WEP, WIFI_CIPHER_WPA, WIFI_CIPHER_NO_PASS, WIFI_CIPHER_INVALID
    }


    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (iWifiConnectListener != null) {
                when (msg.what) {
                    0 -> {
                        val bundle = msg.data
                        val serverIp = bundle.getString("SERVER_IP", "")
                        val selfIp = bundle.getString("SELF_IP", "")
                        iWifiConnectListener!!.onConnectSuccess(selfIp, serverIp)
                    }
                    -1 -> iWifiConnectListener!!.onConnectFail(" fail = " + msg.obj)
                    else -> {}
                }
            }
        }
    }

    fun init(wifiManager: WifiManager?) {
        requireNotNull(wifiManager) { "WifiConnector wifiManager cant be null!" }
        this.wifiManager = wifiManager
    }

    private fun checkInit() {
        requireNotNull(wifiManager) { "You must call init()  before other methods!" }
        requireNotNull(iWifiConnectListener) { "IWifiConnectListener cant be null!" }
    }

    fun connect(
        ssid: String, password: String, type: WifiCipherType, listener: IWifiConnectListener?
    ) {
        iWifiConnectListener = listener
        val thread = Thread(ConnectRunnable(ssid, password, type))
        thread.start()
    }

    inner class ConnectRunnable(
        private val ssid: String, private val password: String, private val type: WifiCipherType
    ) : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            checkInit()
            try {
                // 如果之前没打开wifi,就去打开  确保wifi开关开了
                openWifi()
                iWifiConnectListener?.onConnectStart()
                //开启wifi需要等系统wifi刷新1秒的时间
                Thread.sleep(1000)

                // 如果wifi没开启的话就提示错误
                if (wifiManager!!.wifiState == WifiManager.WIFI_STATE_DISABLED) {
                    sendErrorMsg("WIFI 未开启")
                    return
                }

                // 开启wifi之后开始扫描附近的wifi列表
                wifiManager!!.startScan()
                Thread.sleep(500)
                var hasSsIdWifi = false
                val scanResults: List<ScanResult> = wifiManager!!.scanResults
                Timber.i("目标ssid=$ssid")
                for (i in scanResults.indices) {
                    val scanResult = scanResults[i]
                    if (TextUtils.equals(
                            scanResult.SSID.trim { it <= ' ' },
                            ssid.trim { it <= ' ' })
                    ) {
                        hasSsIdWifi = true
                        break
                    }
                }
                // 如果就没这个wifi的话直接返回
                if (!hasSsIdWifi) {
                    sendErrorMsg("当前不存在指定的Wifi!")
                    return
                }

                //禁掉所有wifi
                for (c in wifiManager!!.configuredNetworks) {
                    wifiManager!!.disableNetwork(c.networkId)
                }

                //看看当前wifi之前配置过没有
                var enabled = false
                val tempConfig: WifiConfiguration? = isExist(ssid)
                Timber.i("tempConfig=${tempConfig}")
                enabled = if (tempConfig != null) {
                    wifiManager!!.enableNetwork(tempConfig.networkId, true)
                } else {
                    val wifiConfig: WifiConfiguration = createWifiInfo(ssid, password, type)
                    val netID: Int = wifiManager!!.addNetwork(wifiConfig)
                    wifiManager!!.enableNetwork(netID, true)
                }
                Log.i(tag, "enable=$enabled")
                if (enabled) {
                    Thread.sleep(2000)
                    val wifiInfo = wifiManager!!.connectionInfo
                    val selfIp = wifiInfo.ipAddress
                    val selfIppAddress = ipAddress2Str(selfIp)
                    Timber.i("address=${selfIppAddress}")
                    val dhcpInfo = wifiManager!!.dhcpInfo
                    val serverAddress = ipAddress2Str(dhcpInfo.serverAddress)
                    Timber.i("serverAddress=$serverAddress")
                    sendSuccessMsg("连接成功! enabled = $enabled", selfIppAddress, serverAddress)
                } else {
                    sendErrorMsg("连接失败! enabled = false")
                }
            } catch (e: Exception) {
                sendErrorMsg(e.message)
                e.printStackTrace()
            }
        }
    }

    private fun ipAddress2Str(selfIp: Int): String {
        val bytes: ByteArray =
            BigInteger.valueOf(selfIp.toLong()).toByteArray().reversed().toByteArray()
        val address: InetAddress = InetAddress.getByAddress(bytes)
        return address.hostName.replace("\\", "")
    }


    fun sendErrorMsg(info: String?) {
        val msg = Message()
        msg.obj = info
        msg.what = -1
        mHandler.sendMessage(msg) // 向Handler发送消息
    }

    fun sendSuccessMsg(info: String?, selfIp: String, serverIp: String) {
        val msg = Message()
        msg.obj = info
        val bundle = Bundle()
        bundle.putString("SERVER_IP", serverIp)
        bundle.putString("SELF_IP", selfIp)
        msg.what = 0
        msg.data = bundle
        mHandler.sendMessage(msg) // 向Handler发送消息
    }

    private fun openWifi(): Boolean {
        checkInit()
        var bRet = true
        if (!wifiManager!!.isWifiEnabled) {
            bRet = wifiManager!!.setWifiEnabled(true)
        }
        return bRet
    }

    // 查看以前是否也配置过这个网络
    @SuppressLint("MissingPermission")
    fun isExist(SSID: String): WifiConfiguration? {
        val existingConfigs = wifiManager?.getConfiguredNetworks()
        if (existingConfigs != null) {
            for (existingConfig in existingConfigs) {
                if (existingConfig.SSID == "\"" + SSID + "\"") {
                    return existingConfig
                }
            }
        }
        return null
    }

    fun createWifiInfo(SSID: String, Password: String, Type: WifiCipherType): WifiConfiguration {
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        config.SSID = "\"" + SSID + "\""
        // config.SSID = SSID;
        // nopass
        if (Type == WifiCipherType.WIFI_CIPHER_NO_PASS) {
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            // config.wepTxKeyIndex = 0;
        } else if (Type == WifiCipherType.WIFI_CIPHER_WEP) { // wep
            if (!TextUtils.isEmpty(Password)) {
                if (isHexWepKey(Password)) {
                    config.wepKeys[0] = Password
                } else {
                    config.wepKeys[0] = "\"" + Password + "\""
                }
            }
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            config.wepTxKeyIndex = 0
        } else if (Type == WifiCipherType.WIFI_CIPHER_WPA) { // wpa
            config.preSharedKey = "\"" + Password + "\""
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            config.status = WifiConfiguration.Status.ENABLED
        }
        return config
    }

    private fun isHexWepKey(wepKey: String): Boolean {
        val len = wepKey.length

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        return if (len != 10 && len != 26 && len != 58) {
            false
        } else isHex(wepKey)
    }

    private fun isHex(key: String): Boolean {
        for (i in key.length - 1 downTo 0) {
            val c = key[i]
            if (!(c in '0'..'9' || c in 'A'..'F' || (c in 'a'..'f'))) {
                return false
            }
        }
        return true
    }


}