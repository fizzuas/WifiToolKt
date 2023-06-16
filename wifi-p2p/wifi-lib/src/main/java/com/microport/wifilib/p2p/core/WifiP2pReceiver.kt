package com.microport.wifilib.p2p.core

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import androidx.core.app.ActivityCompat
import com.microport.wifilib.p2p.api.DirectActionListener
import timber.log.Timber


class WifiP2pReceiver(
    private val mWifiP2pManager: WifiP2pManager, private val mChannel: WifiP2pManager.Channel, private val mDirectActionListener: DirectActionListener
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (action != null) {
            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.wifiP2pEnabled(true)
                    } else {
                        mDirectActionListener.wifiP2pEnabled(false)
                        mDirectActionListener.onPeersAvailable(emptyList())
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (ActivityCompat.checkSelfPermission(
                            context!!, Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
//                    val mPeers: WifiP2pDeviceList? =
//                        intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
//                    if (mPeers != null) {
//                        mDirectActionListener.onPeersAvailable(mPeers.deviceList)
//                    }
                    mWifiP2pManager.requestPeers(mChannel) { peers: WifiP2pDeviceList ->
                        mDirectActionListener.onPeersAvailable(
                            peers.deviceList
                        )
                    }
                }
                //networkInfo、wifiP2pInfo、wifiP2pGroup相关的连接信息，群组信息发生改变
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    //NetworkInfo 的isConnected()可以判断时连接还是断开时接收到的广播
                    val networkInfo: NetworkInfo? = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)

                    //WifiP2pInfo保存着一些连接的信息，如groupFormed字段保存是否有组建立，groupOwnerAddress字段保存GO设备的地址信息，isGroupOwner字段判断自己是否是GO设备。
                    val wifiP2pInfo: WifiP2pInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)

                    //WifiP2pGroup存放着当前组成员的信息，这个信息只有GO设备可以获取。同样这个信息也可以通过wifiP2pManager.requestGroupInfo获取
                    val wifiP2pGroup: WifiP2pGroup? = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)

                    Timber.i(" WIFI_P2P_CONNECTION_CHANGED_ACTION->\nnetworkInfo=${networkInfo}\nwifiP2pInfo=${wifiP2pInfo}\nwifiP2pGroup=${wifiP2pGroup}")

                    if (networkInfo != null && networkInfo.isConnected) {
                        mDirectActionListener.onConnected(wifiP2pInfo, wifiP2pGroup)
                        Timber.e("已连接p2pdf设备")
                    } else {
                        mDirectActionListener.onDisconnected(wifiP2pInfo, wifiP2pGroup)
                        Timber.e("与p2p设备已断开连接")
                    }
                }
                //wifiP2pDevice 状态发生改变
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val wifiP2pDevice = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)

                    Timber.i(" WIFI_P2P_THIS_DEVICE_CHANGED_ACTION->\nwifiP2pDevice=${wifiP2pDevice}")
                    mDirectActionListener.onSelfDeviceChanged(wifiP2pDevice)
                }

                WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                    val discoveryState = intent.getIntExtra(
                        WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED
                    )
                    when (discoveryState) {
                        WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> {
                            Timber.i("discoveryState=WIFI_P2P_DISCOVERY_STARTED")
                        }
                        WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> {
                            Timber.i("discoveryState=WIFI_P2P_DISCOVERY_STOPPED")
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            }
        }
    }
}