package com.microport.hospot_server.page.home.page.permission

import android.Manifest
import android.content.Intent
import com.microport.hospot_server.databinding.ActivityPermissionBinding

import com.microport.hospot_server.base.BaseVmActivity
import com.microport.hospot_server.page.home.page.main.MainActivity
import com.microport.httpserver.util.log.DefaultLogger

class PermissionActivity : BaseVmActivity<ActivityPermissionBinding, PermissionModel>(ActivityPermissionBinding::inflate) {
    private val tag: String = PermissionActivity::class.java.simpleName
    private val log = DefaultLogger()
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )


    override fun viewModelClass(): Class<PermissionModel> = PermissionModel::class.java


    override fun initData() {
        super.initData()
        requestPermission(permissions,
            agree = {
                log.info(tag, "agree")
                startActivity(Intent(this, MainActivity::class.java))

            }, refuse = {
                log.info(tag, "$it not granted!")

            })
    }
}