package com.microport.wifilib.p2p.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.microport.wifilib.p2p.core.WifiP2pReceiver.Companion.getIntentFilter
import com.microport.wifilib.p2p.param.DataFrame
import com.microport.wifilib.p2p.api.*
import com.microport.wifilib.p2p.param.getDeviceStatus
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


class WifiP2pClient(
    private val context: Context, private val deviceChangeListener: WifiDeviceChangeListener?
) : IWifiClient {
    private val mPeriod: Long = 3000
    private val mMainHandler = Handler(Looper.getMainLooper())
    private var wifiP2pEnabled = AtomicBoolean(false)
    private var mDeviceChangeListener: DeviceInfoChangedListener? = null
    private var mGroupInfoChangedListener: GroupInfoChangedListener? = null

    private val mListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            Timber.i("wifiP2pEnabled$enabled")
            wifiP2pEnabled.set(enabled)
        }

        override fun onConnected(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
            Timber.i("onConnected\t${wifiP2pInfo}")
            mGroupInfoChangedListener?.onChanged(wifiP2pInfo, groupInfo)
        }

        override fun onDisconnected(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
            Timber.i("onDisconnected")
            mGroupInfoChangedListener?.onChanged(wifiP2pInfo, groupInfo)
        }

        override fun onSelfDeviceChanged(wifiP2pDevice: WifiP2pDevice?) {
            Timber.i(
                "onSelfDeviceAvailable\t${wifiP2pDevice?.deviceName}\t${
                    getDeviceStatus(
                        wifiP2pDevice!!.status
                    )
                }"
            )
            mDeviceChangeListener?.onChanged(wifiP2pDevice)
        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice?>?) {
            Timber.i("onPeersAvailable->size=${wifiP2pDeviceList?.size ?: "null"}\tthread=${Thread.currentThread().name}")
            deviceChangeListener?.getPeers(wifiP2pDeviceList?.filterNotNull())
        }

        override fun onChannelDisconnected() {
            Timber.i("onChannelDisconnected")
        }
    }

    private val mWifiManager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var mChannel: WifiP2pManager.Channel = mWifiManager.initialize(context, context.mainLooper, mListener)
    private var mReceiver: WifiP2pReceiver = WifiP2pReceiver(mWifiManager, mChannel, mListener)

    init {
        addLifecycleObserver(context)
    }

    private fun addLifecycleObserver(context: Context) {
        (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        context.registerReceiver(mReceiver, getIntentFilter())
                    }
                    Lifecycle.Event.ON_STOP -> {
                        context.unregisterReceiver(mReceiver)
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        stopDiscovery()
                        mMainHandler.removeCallbacksAndMessages(null)
                    }
                    else -> {}
                }
            }
        })
    }


    override fun startDiscovery(searchCallback: SearchCallback?) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            searchCallback?.onFailure(Error.NO_PERMISSION)
            return
        }
        if (!wifiP2pEnabled.get()) {
            searchCallback?.onFailure(Error.WIFI_NO_ENABLE)
            return
        }
        //搜寻附近带有 Wi-Fi P2P 的设备
        mWifiManager.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("onSuccess:\tisMain=${Thread.currentThread() == Looper.getMainLooper().thread}")
                searchCallback?.onStart()
                mMainHandler.postDelayed({
                    searchCallback?.onSuccess()
                }, mPeriod)
            }

            override fun onFailure(p0: Int) {
                Timber.e("onFailure:$p0\tisMain=${Thread.currentThread() == Looper.getMainLooper().thread}")
                searchCallback?.onFailure(transferActionCode(p0))
            }
        })
    }


    override fun stopDiscovery() {
        Timber.i("stopDiscovery")
        mWifiManager.stopPeerDiscovery(mChannel, null)
    }


    override fun connect(device: WifiP2pDevice) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("connect->没有权限")
            return
        }
        if (device.status != WifiP2pDevice.AVAILABLE) {
            Timber.i("connect->连接设备不可连接")
            return
        }

        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        config.wps.setup = WpsInfo.PBC
        Timber.i("正在连接${device.deviceName} ......")
        mWifiManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("connect action success")
            }

            override fun onFailure(p0: Int) {
                Timber.e("connect action fail p0=${p0}")
            }
        })
    }


    fun cancelConnect(device: WifiP2pDevice) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.i("connect->没有权限")
            return
        }
        if (device.status != WifiP2pDevice.INVITED) {
            Timber.i("connect->连接设备不可取消")
            return
        }

        Timber.i("正在取消${device.deviceName} ......")
        mWifiManager.cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("cancelConnect action success")
            }

            override fun onFailure(p0: Int) {
                Timber.e("cancelConnect action fail p0=${p0}")
            }
        })
    }

    override fun disConnect(callback: DisConnectCallback) {
        mWifiManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Timber.i("disConnect action success")
                callback.onSuccess()
            }

            override fun onFailure(p0: Int) {
                Timber.e("disConnect action fail p0=${p0}")
                callback.onFail(transferActionCode(p0))
            }
        })
    }

    override fun send(dataFrame: DataFrame) {
        TODO("Not yet implemented")
    }

    fun addDeviceChangedListener(deviceChangeListener: DeviceInfoChangedListener) {
        mDeviceChangeListener = deviceChangeListener
    }

    fun addGroupInfoChangeListener(groupInfoChangedListener: GroupInfoChangedListener) {
        mGroupInfoChangedListener = groupInfoChangedListener
    }

}