package com.microport.wifi.client.page.p2p

import android.app.ProgressDialog
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.microport.wifi.client.R
import com.microport.wifi.client.databinding.ActivitySendBinding
import com.microport.wifi.client.task.WifiClientTask
import com.microport.wifilib.p2p.api.*
import com.microport.wifilib.p2p.core.WifiP2pClient
import com.microport.wifilib.p2p.param.DeviceInfo
import com.microport.wifilib.p2p.param.getDeviceStatus
import timber.log.Timber

class SendActivity : AppCompatActivity() {

    lateinit var mWifeClient: WifiP2pClient
    lateinit var binding: ActivitySendBinding
    private val mWifiDevices = mutableListOf<DeviceInfo>()
    lateinit var mAdapter: WifiDevicesAdapter

    private var mWifiP2pInfo: WifiP2pInfo? = null


    private val mPeersChangeListener: WifiDeviceChangeListener = object : WifiDeviceChangeListener {
        override fun getPeers(wifiP2pDeviceList: Collection<WifiP2pDevice>?) {
            wifiP2pDeviceList?.forEach {
                Timber.i("getPeer:" + it.deviceName + "\tstatus=${getDeviceStatus(it.status)}")
            }
            mWifiDevices.clear()
            wifiP2pDeviceList?.let {
                mWifiDevices.addAll(wifiP2pDeviceList.map { DeviceInfo(device = it) }
                    .sortedBy { it.device.status })
            }
            mAdapter.setList(mWifiDevices)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mWifeClient = WifiP2pClient(this, mPeersChangeListener)

        val progressDialog = ProgressDialog(this)
        binding.butSearch.setOnClickListener {
            mWifeClient.startDiscovery(object : SearchCallback {
                override fun onStart() {
                    Timber.i("start search wifi...")
                    progressDialog.show()
                }

                override fun onSuccess() {
                    progressDialog.cancel()
                }

                override fun onFailure(error: Error) {
                    progressDialog.cancel()
                    showToast(error.getMsg())
                }
            })
        }

        mAdapter = WifiDevicesAdapter(data = mWifiDevices)
        mAdapter.addChildClickViewIds(R.id.item_container)
        mAdapter.addChildLongClickViewIds(R.id.item_container)
        binding.recy.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@SendActivity)
            itemAnimator = DefaultItemAnimator()
        }
        mAdapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.item_container) {
                val deviceInfo = mAdapter.data[position]
                when (deviceInfo.device.status) {
                    WifiP2pDevice.AVAILABLE -> {
                        mWifeClient.connect(deviceInfo.device)
                    }
                    WifiP2pDevice.INVITED -> {
                        mWifeClient.cancelConnect(device = deviceInfo.device)
                    }
                }
            }
        }

        binding.butDisconnect.setOnClickListener {
            mWifeClient.disConnect(object : DisConnectCallback {
                override fun onSuccess() {
                    showToast("disconnect suc")
                }

                override fun onFail(error: Error) {
                    showToast("disconnect fail ${error.getMsg()}")
                }
            })
        }
        binding.butSend.setOnClickListener {
            navToChosePicture()
        }
        mWifeClient.addDeviceChangedListener(object : DeviceInfoChangedListener {
            override fun onChanged(selfDevice: WifiP2pDevice?) {
                val stringBuilder = StringBuilder()
                if (selfDevice != null) {
                    stringBuilder.append("本设备名：")
                    stringBuilder.append(selfDevice.deviceName)
                    stringBuilder.append("\n")
                    stringBuilder.append("本设备的地址：")
                    stringBuilder.append(selfDevice.deviceAddress)
                    stringBuilder.append("\n")
                    stringBuilder.append("本设备状态：")
                    stringBuilder.append(getDeviceStatus(selfDevice.status))
                }
                binding.tvDevice.text = stringBuilder.toString()
            }
        })

        mWifeClient.addGroupInfoChangeListener(object : GroupInfoChangedListener {
            override fun onChanged(wifiP2pInfo: WifiP2pInfo?, groupInfo: WifiP2pGroup?) {
                mWifiP2pInfo =wifiP2pInfo
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

    fun showToast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navToChosePicture() {
        val intent = Intent(Intent.ACTION_PICK, null)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(intent, 100)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                val imageUri = data!!.data
                if (mWifiP2pInfo != null) {
                    Timber.i("\"文件路径：$imageUri\thostAddress=${mWifiP2pInfo!!.groupOwnerAddress.hostAddress}")
                    WifiClientTask(this).execute(
                        mWifiP2pInfo!!.groupOwnerAddress.hostAddress, imageUri
                    )
                }
            }
        }
    }
}