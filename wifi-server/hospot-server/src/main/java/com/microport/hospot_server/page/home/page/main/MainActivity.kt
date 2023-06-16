package com.microport.hospot_server.page.home.page.main

import android.annotation.SuppressLint
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import com.microport.hospot_server.databinding.ActivityMainBinding
import com.microport.hospot_server.page.home.adapter.ConnectInfoListAdapter
import com.microport.hospot_server.base.BaseVmActivity
import com.microport.hospot_server.page.home.page.transport.TransportActivity
import com.microport.hospot_server.util.WifiUtil
import com.microport.httpserver.facade.interfaces.ConnectChangeListener
import com.microport.httpserver.facade.interfaces.IHttpServer
import com.microport.httpserver.facade.interfaces.StartServerCallback
import com.microport.httpserver.launch.HttpServer
import com.microport.httpserver.util.log.DefaultLogger


class MainActivity : BaseVmActivity<ActivityMainBinding, MainModel>(ActivityMainBinding::inflate) {
    private val mHttpServer: IHttpServer = HttpServer.getInstance()
    private val logger = DefaultLogger()
    private val tag = MainActivity::class.java.simpleName

    private val mClientMessages = mutableListOf<String>()
    private val mClientMessagesAdapter = ConnectInfoListAdapter(list = mClientMessages)
    override fun viewModelClass(): Class<MainModel> = MainModel::class.java


    override fun initData() {
        super.initData()
        mBinding.recyclerViewConnect.apply {
            adapter = mClientMessagesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        mBinding.btnOpenHotspot.setOnClickListener {
            val apAddress = WifiUtil.getWifiApIpAddress()
            logger.info(tag, "ApAddress = $apAddress")
            mBinding.tvOpenHotspotRst.text = "ApAddress=$apAddress"
        }

        mBinding.btnStartServer.setOnClickListener {
            mHttpServer.startServer(callback = object : StartServerCallback {
                @SuppressLint("SetTextI18n")
                override fun onSuccess() {
                    logger.info(tag, "Server start Success")
                    runOnUiThread {
                        mBinding.tvStartServerRst.text = "Server start Success "
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onFail(msg: String?) {
                    logger.error(tag, "Server start fail because of [$msg]")
                    mBinding.tvStartServerRst.text = "Server start fail because of [$msg]"
                }
            })
        }

        mBinding.butTransport.setOnClickListener {
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