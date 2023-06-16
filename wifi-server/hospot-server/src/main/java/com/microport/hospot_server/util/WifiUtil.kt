/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.util

import android.app.Application
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.microport.httpserver.util.log.DefaultLogger
import java.lang.reflect.Method
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


/**
 * Wifi工具类
 *
 * @author ouyx
 * @date 2023年04月12日 11时01分
 */
object WifiUtil {
    private val logger = DefaultLogger()
    private val tag = WifiUtil::class.java.simpleName

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openHotSpot(application: Application, mainHandler: Handler) {
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                val wifiConfiguration = reservation.wifiConfiguration
                if (wifiConfiguration != null) {
                    logger.info(tag, "start wifi hotspot  cfg[ssid=${wifiConfiguration.SSID},pwd=${wifiConfiguration.preSharedKey}]")
                }
                logger.info(tag, "onSuccess")
            }

            override fun onStopped() {
                super.onStopped()
                logger.info(tag, "onStopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                logger.info(tag, "onFailed")

            }
        }, mainHandler)

    }


    fun getWifiApIpAddress(): String? {
        try {
            val enumeration: Enumeration<NetworkInterface> = NetworkInterface
                .getNetworkInterfaces()
            while (enumeration.hasMoreElements()) {
                val networkInterface: NetworkInterface = enumeration.nextElement()
                if (networkInterface.name.contains("wlan")) {
                    val enumIpAddress: Enumeration<InetAddress> = networkInterface
                        .inetAddresses
                    while (enumIpAddress.hasMoreElements()) {
                        val inetAddress: InetAddress = enumIpAddress.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress.address.size === 4) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
            logger.error(tag, ex.toString())
        }
        return null
    }
}