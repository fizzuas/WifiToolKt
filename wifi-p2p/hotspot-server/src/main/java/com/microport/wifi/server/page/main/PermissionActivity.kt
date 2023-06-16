package com.microport.wifi.server.page.main

import android.Manifest
import android.content.Intent
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.interfaces.PermissionCallBack
import com.microport.wifi.server.databinding.ActivityServerMainBinding
import com.microport.wifi.server.page.server.hotspot.HotSpotActivity
import com.microport.wifi.server.page.server.p2p.WifiServerActivity

class PermissionActivity : BaseVBActivity<PermissionModel, ActivityServerMainBinding>() {

    override fun createViewModel() = PermissionModel(application)

    override fun initData() {
        super.initData()
        requestPermission(arrayOf(
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ), object : PermissionCallBack {
            override fun agree() {
                val intent = Intent(this@PermissionActivity, HotSpotActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun refuse(refusePermissions: ArrayList<String>) {}
        })
    }
}