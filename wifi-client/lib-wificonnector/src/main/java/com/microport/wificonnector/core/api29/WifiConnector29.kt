/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.wificonnector.core.api29

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.microport.wificonnector.util.log.DefaultLogger


/**
 * AAndroid  sdk >=28 使用的连接API
 *
 * @author admin
 * @date 2023年04月07日 15时16分
 */
class WifiConnector29 {
    private val tag = WifiConnector29::class.java.simpleName
    private val logger = DefaultLogger()
    private fun connect(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = WifiNetworkSpecifier.Builder()
            builder.setSsid("ouyx")
            builder.setWpa2Passphrase("123456789")

            val wifiNetworkSpecifier = builder.build()

            val networkRequestBuilder1 = NetworkRequest.Builder()
            networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier)

            val nr = networkRequestBuilder1.build()
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.requestNetwork(nr, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    logger.info(tag, "onAvailable->${network}")
                    cm.bindProcessToNetwork(network)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    logger.info(tag, "onUnavailable")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    logger.info(tag, "onLost->${network}")
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    logger.info(tag, "onLosing->${network}")
                }

                override fun onCapabilitiesChanged(
                    network: Network, networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    logger.info(tag, "onCapabilitiesChanged->${network}")
                }

                override fun onLinkPropertiesChanged(
                    network: Network, linkProperties: LinkProperties
                ) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                    logger.info(tag, "onLinkPropertiesChanged->${network}")
                }

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    super.onBlockedStatusChanged(network, blocked)
                    logger.info(tag, "onBlockedStatusChanged->${network}\tblocked=$blocked")

                }
            })
        }
    }

}