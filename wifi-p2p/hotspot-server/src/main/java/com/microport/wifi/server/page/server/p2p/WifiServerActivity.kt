package com.microport.wifi.server.page.server.p2p

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.microport.wifi.server.databinding.ActivityServerBinding
import com.microport.wifi.server.page.server.service.OnProgressChangListener
import com.microport.wifi.server.page.server.service.WifiServerService
import com.microport.wifi.server.page.server.service.WifiServerService.WifiServerBinder
import com.microport.wifilib.p2p.api.CreateGroupCallBack
import com.microport.wifilib.p2p.api.DeviceInfoChangedListener
import com.microport.wifilib.p2p.api.GroupInfoChangedListener
import com.microport.wifilib.p2p.api.RemoveGroupCallBack
import com.microport.wifilib.p2p.core.WifiServer
import com.microport.wifilib.p2p.param.getDeviceStatus
import github.leavesc.wifip2p.model.FileTransfer
import timber.log.Timber
import java.io.File

class WifiServerActivity : AppCompatActivity() {
    private lateinit var mWifiServer: WifiServer
    lateinit var binding: ActivityServerBinding
    private var wifiServerService: WifiServerService? = null


    val mProgressListener = object : OnProgressChangListener {
        override fun onProgressChanged(fileTransfer: FileTransfer?, progress: Int) {
        }

        override fun onTransferFinished(file: File?) {
            Timber.i("onTransferFinished：${file?.name}")
            runOnUiThread {
                if (file != null && file.exists()) {
                    Glide.with(this@WifiServerActivity).load(file.path).into(binding.img)
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.i("onServiceConnected \t${name}")
            val binder = service as WifiServerBinder
            wifiServerService = binder.service
            wifiServerService?.setProgressChangListener(mProgressListener)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.i("onServiceDisconnected \t${name}")
            wifiServerService?.setProgressChangListener(null)
            wifiServerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mWifiServer = WifiServer(this)
        initView()
        bindService()
    }

    private fun initView() {
        binding.butCreateGroup.setOnClickListener {
            mWifiServer.createGroup(object : CreateGroupCallBack {
                override fun onSuccess(wifiP2pInfo: WifiP2pInfo?) {
                    Toast.makeText(this@WifiServerActivity, "success", Toast.LENGTH_SHORT).show()
                    wifiServerService?.let {
                        val intent = Intent(this@WifiServerActivity, WifiServerService::class.java)
                        startService(intent)
                    }

                }

                override fun onError(error: com.microport.wifilib.p2p.api.Error) {
                    Toast.makeText(this@WifiServerActivity, error.getMsg(), Toast.LENGTH_SHORT)
                        .show()
                }

            })
        }
        binding.butDelGroup.setOnClickListener {
            mWifiServer.removeGroup(object : RemoveGroupCallBack {
                override fun onSuccess() {
                    Toast.makeText(this@WifiServerActivity, "remove success", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onError(error: com.microport.wifilib.p2p.api.Error) {
                    Toast.makeText(this@WifiServerActivity, error.getMsg(), Toast.LENGTH_SHORT)
                        .show()
                }

            })
        }

        mWifiServer.addDeviceChangedListener(object : DeviceInfoChangedListener {
            override fun onChanged(selfDeviceInfo: WifiP2pDevice?) {
                val stringBuilder = StringBuilder()
                if (selfDeviceInfo != null) {
                    stringBuilder.append("本设备名：")
                    stringBuilder.append(selfDeviceInfo.deviceName)
                    stringBuilder.append("\n")
                    stringBuilder.append("本设备的地址：")
                    stringBuilder.append(selfDeviceInfo.deviceAddress)
                    stringBuilder.append("\n")
                    stringBuilder.append("本设备状态：")
                    stringBuilder.append(getDeviceStatus(selfDeviceInfo.status))
                }

                binding.tvDeviceInfo.text = stringBuilder.toString()
            }
        })

        mWifiServer.addGroupInfoChangeListener(object :GroupInfoChangedListener{
            override fun onChanged(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
                val stringBuilder = StringBuilder()
                if (wifiP2pInfo != null) {
                    stringBuilder.append("是否群主：")
                    stringBuilder.append(if (wifiP2pInfo.isGroupOwner) "是群主" else "非群主")
                    stringBuilder.append("\n")
                    stringBuilder.append("群主IP地址：")
                    stringBuilder.append(wifiP2pInfo.groupOwnerAddress?.hostAddress)
                }
                groupInfo?.let {
                    stringBuilder.append("\n\n")
                    stringBuilder.append("群成员: ${groupInfo.clientList}")
                }

                binding.tvGroupInfo.text = stringBuilder.toString()
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        stopService(Intent(this, WifiServerService::class.java))
    }

    private fun bindService() {
        val intent = Intent(this, WifiServerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }
}