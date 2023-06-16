package com.microport.wifi_client.page.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bhm.support.sdk.common.BaseVBActivity
import com.microport.wifi_client.databinding.ActivityClientMainBinding

class PermissionActivity : BaseVBActivity<PermissionViewModel, ActivityClientMainBinding>() {
    private val tag = PermissionActivity::class.java.simpleName

    override fun createViewModel() = PermissionViewModel(application)

    override fun initData() {
        super.initData()
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val rst = ContextCompat.checkSelfPermission(application, permissions[0]) == PackageManager.PERMISSION_GRANTED
        if (rst) {
            toWifiPage()
        } else {
            this.requestPermissions(permissions, 0x01)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0x01) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toWifiPage()
            }
        }
    }

    private fun toWifiPage() {
        startActivity(Intent(this, HotSpotClientActivity::class.java))
    }
}