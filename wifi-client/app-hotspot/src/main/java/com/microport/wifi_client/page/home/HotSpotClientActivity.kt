package com.microport.wifi_client.page.home

import android.content.Intent
import android.widget.Button
import com.microport.lib_httpclient.constant.Constant
import com.microport.wificonnector.constant.Constant.CONNECT_FINISH
import com.microport.wificonnector.constant.Constant.DISCONNECTED
import com.microport.wificonnector.constant.Constant.ERROR_CONNECT
import com.microport.wificonnector.constant.Constant.ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG
import com.microport.wificonnector.constant.Constant.ERROR_DEVICE_NOT_HAVE_WIFI
import com.microport.wificonnector.constant.Constant.ERROR_PASSWORD
import com.microport.wifi_client.databinding.ActivityHotSpotBinding
import com.microport.lib_httpclient.facade.ConnectClientCallback
import com.microport.lib_httpclient.launch.HttpClient
import com.microport.wifi_client.R
import com.microport.wifi_client.base.BaseActivity
import com.microport.wifi_client.base.BaseVmActivity
import com.microport.wifi_client.page.transport.TransportActivity
import com.microport.wifi_client.util.log.DefaultLogger
import com.microport.wificonnector.core.api.WifiApiTools
import com.microport.wificonnector.core.api.WifiConnector
import com.microport.wificonnector.facade.OnWifiConnectStatusChangeListener
import com.microport.wificonnector.facade.WifiInfo


class HotSpotClientActivity : BaseVmActivity<ActivityHotSpotBinding, HotSpotViewModel>(ActivityHotSpotBinding::inflate) {
    private val logger = DefaultLogger()
    private val tag = HotSpotClientActivity::class.java.simpleName
    private lateinit var mWifiUtil: WifiConnector
    override fun viewModelClass(): Class<HotSpotViewModel> = HotSpotViewModel::class.java


    override fun initData() {
        super.initData()

        mWifiUtil = WifiConnector(this)
        initView()
    }


    override fun initView() {
        mBinding.btnConnectWifi.setOnClickListener {
            mWifiUtil.connect("ouyx", "123456789", WifiApiTools.WifiCipherType.WIFICIPHER_WPA, object : OnWifiConnectStatusChangeListener() {
                override fun onStatusChange(isSuccess: Boolean, statusCode: Int) {
                    logger.info(tag, "isSuccess = $isSuccess  statueCode=$statusCode")
                    when (statusCode) {
                        ERROR_DEVICE_NOT_HAVE_WIFI -> mBinding.txtLog.text = "错误：设备无Wifi"
                        ERROR_CONNECT -> mBinding.txtLog.text = "错误：连接失败"
                        ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG -> mBinding.txtLog.text = "错误：设备已存在相同Wifi配置"
                        ERROR_PASSWORD -> mBinding.txtLog.text = "错误：密码错误"
                        CONNECT_FINISH -> mBinding.txtLog.text = "已连接"
                        DISCONNECTED -> mBinding.txtLog.text = "已断开连接"
                    }
                }

                override fun onConnect(wifiInfo: WifiInfo) {
                    logger.info(tag, wifiInfo.toString())
                    mBinding.txtLog.text = "已连接$wifiInfo"
                    if (wifiInfo.gateWay != null) {
                        mBinding.editServerIp.setText(wifiInfo.gateWay)
                    }
                }
            })
        }
        mBinding.btnDisconnectWifi.setOnClickListener {
            mWifiUtil.disconnectWifi()
        }

        mBinding.btnConnectServer.setOnClickListener {
            mBinding.tvConnectRst.text = "连接.."
            val host = mBinding.editServerIp.text.toString()
            HttpClient.getInstance().connectSever(host = host, port = Constant.SERVER_DEFAULT_PORT, object : ConnectClientCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        mBinding.tvConnectRst.text = "连接成功"
                    }
                }

                override fun onFail(msg: String?) {
                    runOnUiThread {
                        mBinding.tvConnectRst.text = "连接失败"

                    }
                }
            })
        }
        mBinding.btnDisconnectServer.setOnClickListener {
            HttpClient.getInstance().disConnect()
        }
        mBinding.btnSend.setOnClickListener {
            startActivity(Intent(this, TransportActivity::class.java))
            HttpClient.getInstance().executeTask {
                logger.info(tag, " update ui ${it}")
//                mBinding.btnSend.text = "hah"
                findViewById<Button>(R.id.but_send_images).text = "hah"
            }
        }
    }
}