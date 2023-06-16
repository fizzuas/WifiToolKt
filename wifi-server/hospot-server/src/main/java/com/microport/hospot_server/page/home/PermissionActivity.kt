package com.microport.hospot_server.page.home

import android.Manifest
import android.content.Intent
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.interfaces.PermissionCallBack
import com.microport.hospot_server.databinding.ActivityServerBinding
import com.microport.httpserver.util.log.DefaultLogger

class PermissionActivity : BaseVBActivity<PermissionModel, ActivityServerBinding>() {
    val tag: String = PermissionActivity::class.java.simpleName
    val log = DefaultLogger()
    override fun createViewModel() = PermissionModel(application)

    override fun initData() {
        super.initData()
        log.info(tag, "initData")
        requestPermission(arrayOf(
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ), object : PermissionCallBack {
            override fun agree() {
                log.info(tag, "agree")
                val intent = Intent(this@PermissionActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun refuse(refusePermissions: ArrayList<String>) {
                log.info(tag, "refuse")
            }
        })
    }
}