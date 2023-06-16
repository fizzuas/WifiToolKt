package com.microport.wifi_client.base

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.gyf.immersionbar.ktx.immersionBar
import com.microport.wifi_client.R

abstract class BaseActivity<VB : ViewBinding>(private val inflate: (LayoutInflater) -> VB) : AppCompatActivity() {
    protected open lateinit var mBinding: VB
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissionAgree: (() -> Unit)? = null
    private var permissionRefuse: ((refusePermissions: List<String>) -> Unit)? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = inflate(layoutInflater)
        setContentView(mBinding.root)
        //竖屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        immersionBar {
            fitsSystemWindows(true)
            statusBarColor(R.color.colorPrimary)
            statusBarDarkFont(true)
            navigationBarColor(R.color.transparent)
        }

        initialize()
    }

    open fun initialize() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val refusePermissions = permissions.filterNot { it.value }.keys.toList()
            if (refusePermissions.isEmpty()) {
                permissionAgree?.invoke()
            } else {
                permissionRefuse?.invoke(refusePermissions)
            }
        }

    }

    protected fun requestPermission(permissions: Array<String>, agree: () -> Unit, refuse: (refusePermissions: List<String>) -> Unit) {
        this.permissionAgree = agree
        this.permissionRefuse = refuse
        val notGrantedPermissions = permissions.filterNot { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (notGrantedPermissions.isEmpty()) {
            permissionAgree?.invoke()
            return
        }
        permissionLauncher?.launch(permissions)
    }


}