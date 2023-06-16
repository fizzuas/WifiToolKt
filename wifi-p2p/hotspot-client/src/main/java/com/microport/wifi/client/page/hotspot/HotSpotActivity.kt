package com.microport.wifi.client.page.hotspot

import android.app.ProgressDialog
import android.content.Intent
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.microport.wifi.client.databinding.ActivityHotSpotBinding
import com.microport.wifi.client.task.WifiClientTask
import com.microport.wifilib.hotspot.api.IWifiConnectListener
import com.microport.wifilib.hotspot.core.WifiManagerProxy
import timber.log.Timber


class HotSpotActivity : AppCompatActivity() {
    private lateinit var mViewBinding: ActivityHotSpotBinding
    private var mServerAddress: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityHotSpotBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        initView()
    }

    private fun initView() {
        mViewBinding.butAutoConnect.setOnClickListener {
            connectBefore10()
        }
        mViewBinding.butAutoConnect2.setOnClickListener {
            connectAfterAnd10()
        }

        mViewBinding.butTraFile.setOnClickListener {
            navToChosePicture()
        }
    }

    private fun connectAfterAnd10() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = WifiNetworkSpecifier.Builder()
            builder.setSsid("ouyx")
            builder.setWpa2Passphrase("123456789")

            val wifiNetworkSpecifier = builder.build()

            val networkRequestBuilder1 = NetworkRequest.Builder()
            networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier)

            val nr = networkRequestBuilder1.build()
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.requestNetwork(nr, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Timber.i("onAvailable->${network}")
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Timber.i("onUnavailable")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Timber.i("onLost->${network}")
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    Timber.i("onLosing->${network}")
                }

                override fun onCapabilitiesChanged(
                    network: Network, networkCapabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    Timber.i("onCapabilitiesChanged->${network}")
                }

                override fun onLinkPropertiesChanged(
                    network: Network, linkProperties: LinkProperties
                ) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                    Timber.i("onLinkPropertiesChanged->${network}")
                }

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    super.onBlockedStatusChanged(network, blocked)
                    Timber.i("onBlockedStatusChanged->${network}\tblocked=$blocked")

                }
            })
        }
    }


    private fun connectBefore10() {
        val dialog = ProgressDialog(this)
        dialog.setTitle("正在连接")
        dialog.show()
        WifiManagerProxy.instance.init(application)
        WifiManagerProxy.instance.connect("ouyx", "123456789", object : IWifiConnectListener {
            override fun onConnectStart() {
            }

            override fun onConnectSuccess(selfIP: String, serverIP: String) {
                Timber.i("connect success\t selfIp=$selfIP \t serverIP=$serverIP ")
                dialog.dismiss()
                Toast.makeText(this@HotSpotActivity, "连接成功", Toast.LENGTH_SHORT).show()
                mServerAddress = serverIP
            }


            override fun onConnectFail(errorMsg: String?) {
                Timber.i("connect fail $errorMsg")
                dialog.dismiss()
                Toast.makeText(this@HotSpotActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        })
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
                if (mServerAddress != null) {
                    Timber.i("\"文件路径：$imageUri\thostAddress=${mServerAddress}")
                    WifiClientTask(this).execute(
                        mServerAddress, imageUri
                    )
                }
            }
        }
    }


}