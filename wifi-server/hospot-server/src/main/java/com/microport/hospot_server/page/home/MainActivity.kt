package com.microport.hospot_server.page.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.core.AppTheme
import com.microport.hospot_server.databinding.ActivityMainBinding
import com.microport.hospot_server.page.home.adapter.ConnectInfoListAdapter
import com.microport.hospot_server.page.transport.TransportActivity
import com.microport.hospot_server.util.WifiUtil
import com.microport.httpserver.facade.interfaces.ConnectChangeListener
import com.microport.httpserver.facade.interfaces.IHttpServer
import com.microport.httpserver.facade.interfaces.StartServerCallback
import com.microport.httpserver.launch.HttpServer
import com.microport.httpserver.util.log.DefaultLogger


class MainActivity : BaseVBActivity<MainModel, ActivityMainBinding>() {
    private val mHttpServer: IHttpServer = HttpServer.getInstance()
    private val logger = DefaultLogger()
    private val tag = MainActivity::class.java.simpleName

    private val mClientMessages = mutableListOf<String>()
    private val mClientMessagesAdapter = ConnectInfoListAdapter(list = mClientMessages)


    override fun createViewModel(): MainModel = MainModel(application)
    override fun initData() {
        super.initData()
        AppTheme.setLightStatusBar(this)
        viewBinding.recyclerViewConnect.apply {
            adapter = mClientMessagesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        viewBinding.btnOpenHotspot.setOnClickListener {
            val apAddress = WifiUtil.getWifiApIpAddress()
            logger.info(tag, "ApAddress = $apAddress")
            viewBinding.tvOpenHotspotRst.text = "ApAddress=$apAddress"
        }

        viewBinding.btnStartServer.setOnClickListener {
            mHttpServer.startServer(callback = object : StartServerCallback {
                @SuppressLint("SetTextI18n")
                override fun onSuccess() {
                    logger.info(tag, "Server start Success")
                    runOnUiThread {
                        viewBinding.tvStartServerRst.text = "Server start Success "
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onFail(msg: String?) {
                    logger.error(tag, "Server start fail because of [$msg]")
                    viewBinding.tvStartServerRst.text = "Server start fail because of [$msg]"
                }
            })
        }

        viewBinding.butTransport.setOnClickListener {
            val intent = Intent(this, TransportActivity::class.java)
            startActivity(intent)
        }

        mHttpServer.setConnectChangeListener(object : ConnectChangeListener {
            override fun onConnected(id: String?, clientIP: String?) {
                mClientMessages.add("id=$id  clientIP=$clientIP 连接上了")
                mClientMessagesAdapter.notifyItemInserted(mClientMessages.lastIndex)
            }

            override fun onDisConnect(id: String?, clientIP: String?) {
                mClientMessages.add("id=$id  clientIP=$clientIP 断开了")
                mClientMessagesAdapter.notifyItemInserted(mClientMessages.lastIndex)
            }
        })
    }
}