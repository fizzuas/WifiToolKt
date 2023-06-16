package com.microport.wifi.client.page.hotspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.net.wifi.p2p.WifiP2pManager

class WifiHotSpotReceiver (private val mWifiP2pManager: WifiP2pManager): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

    }

    companion object {
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply {
            }
        }
    }
}