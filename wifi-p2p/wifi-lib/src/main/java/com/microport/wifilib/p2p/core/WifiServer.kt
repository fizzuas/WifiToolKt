package com.microport.wifilib.p2p.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.microport.wifilib.p2p.api.*
import com.microport.wifilib.p2p.param.getDeviceStatus
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class WifiServer(private val context: Context) : IWifiServer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var wifiP2pEnabled = AtomicBoolean(false)
    private var mDeviceChangedListener: DeviceInfoChangedListener? = null
    private var mGroupInfoChangedListener: GroupInfoChangedListener? = null

    private val mListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            Timber.i("wifiP2pEnabled$enabled")
            wifiP2pEnabled.set(enabled)
        }

        override fun onConnected(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
            Timber.i("onConnected\t${wifiP2pInfo}")
            mCreateGroupCallBack?.onSuccess(wifiP2pInfo)
            mCreateGroupCallBack = null

            mGroupInfoChangedListener?.onChanged(wifiP2pInfo, groupInfo)
        }

        override fun onDisconnected(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
            Timber.i("onDisconnected")
            mRemoveGroupCallBack?.onSuccess()
            mRemoveGroupCallBack = null

            mGroupInfoChangedListener?.onChanged(wifiP2pInfo, groupInfo)
        }

        override fun onSelfDeviceChanged(wifiP2pDevice: WifiP2pDevice?) {
            Timber.i(
                "onSelfDeviceChanged\t${wifiP2pDevice?.deviceName}\t${
                    getDeviceStatus(
                        wifiP2pDevice!!.status
                    )
                }"
            )
            mDeviceChangedListener?.onChanged(wifiP2pDevice)
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice?>?) {
            Timber.i("onPeersAvailable->size=${wifiP2pDeviceList?.size ?: "null"}\tthread=${Thread.currentThread().name}")
        }

        override fun onChannelDisconnected() {
            Timber.i("onChannelDisconnected")
        }
    }

    private val mWifiManager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var mChannel: WifiP2pManager.Channel = mWifiManager.initialize(context, context.mainLooper, mListener)
    private var mReceiver: WifiP2pReceiver = WifiP2pReceiver(mWifiManager, mChannel, mListener)


    private var mCreateGroupCallBack: CreateGroupCallBack? = null
    private var mRemoveGroupCallBack: RemoveGroupCallBack? = null

    init {
        addLifecycleObserver(context)
    }

    private fun addLifecycleObserver(context: Context) {
        (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        context.registerReceiver(mReceiver, WifiP2pReceiver.getIntentFilter())
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        context.unregisterReceiver(mReceiver)
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        mWifiManager.removeGroup(mChannel, null)
                        mainHandler.removeCallbacksAndMessages(null)
                    }
                    else -> {}
                }
            }
        })
    }

    override fun createGroup(callBack: CreateGroupCallBack) {
        if (mCreateGroupCallBack != null) {
            callBack.onError(Error.BUSY)
            return
        }
        mCreateGroupCallBack = callBack
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mCreateGroupCallBack?.onError(Error.NO_PERMISSION)
            mCreateGroupCallBack = null
            return
        }
        if (!wifiP2pEnabled.get()) {
            mCreateGroupCallBack?.onError(Error.WIFI_NO_ENABLE)
            mCreateGroupCallBack = null
            return
        }
        mWifiManager.createGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("createGroup success isMain=${Thread.currentThread() == Looper.getMainLooper().thread}")
            }

            override fun onFailure(p0: Int) {
                Timber.e("createGroup fail$p0\t isMain=${Thread.currentThread() == Looper.getMainLooper().thread}")
                mCreateGroupCallBack?.onError(transferActionCode(p0))
                mCreateGroupCallBack = null
            }
        })
    }


    override fun removeGroup(callBack: RemoveGroupCallBack) {
        if (mRemoveGroupCallBack != null) {
            callBack.onError(Error.BUSY)
            return
        }
        mRemoveGroupCallBack = callBack
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mRemoveGroupCallBack?.onError(Error.NO_PERMISSION)
            mRemoveGroupCallBack = null
            return
        }
        if (!wifiP2pEnabled.get()) {
            mRemoveGroupCallBack?.onError(Error.WIFI_NO_ENABLE)
            mRemoveGroupCallBack = null
            return
        }
        mWifiManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("removeGroup success")
            }

            override fun onFailure(p0: Int) {
                Timber.e("removeGroup fail$p0")
                mRemoveGroupCallBack?.onError(transferActionCode(p0))
                mRemoveGroupCallBack = null
            }
        })
    }

    fun removeGroup() {
        mWifiManager.removeGroup(mChannel, null)
    }

    fun addDeviceChangedListener(deviceChangeListener: DeviceInfoChangedListener) {
        mDeviceChangedListener = deviceChangeListener
    }

    fun addGroupInfoChangeListener(groupInfoChangedListener: GroupInfoChangedListener) {
        mGroupInfoChangedListener = groupInfoChangedListener
    }

}




