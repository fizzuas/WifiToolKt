/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.lib_httpclient.launch

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.microport.lib_httpclient.core.netty.HttpClientViaNetty
import com.microport.lib_httpclient.facade.ConnectChangeListener
import com.microport.lib_httpclient.facade.ConnectClientCallback
import com.microport.lib_httpclient.facade.IHttpClient
import com.microport.lib_httpclient.facade.PackageReceivedListener
import com.microport.wifi_client.util.log.DefaultLogger
import kotlin.concurrent.thread


/**
 *
 * @author ouyx
 * @date 2023年04月17日 17时51分
 */
class HttpClient private constructor() : IHttpClient {
    private val mDelegate = HttpClientViaNetty()
    private val tag = HttpClient::class.java.simpleName
    private val logger = DefaultLogger()

    companion object {
        val tag = HttpClient::class.simpleName!!
        private var instance: HttpClient? = null
        fun getInstance(): HttpClient {
            if (instance == null) {
                synchronized(HttpClient::class.java) {
                    if (instance == null) {
                        instance = HttpClient()
                    }
                }
            }
            return instance!!
        }
    }

    override fun connectSever(host: String, port: Int, callback: ConnectClientCallback?) {
        mDelegate.connectSever(host, port, callback)

    }

    override fun disConnect() {
        mDelegate.disConnect()
    }

    override fun sendPackage(packet: com.microport.lib_httpclient.contract.Packet, callback: (Boolean) -> Unit) {
        mDelegate.sendPackage(packet, callback)

    }


    override fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit) {
        mDelegate.sendBytes(data, callback)
    }

    override fun setReceivePackageListener(listener: PackageReceivedListener?) {
        mDelegate.setReceivePackageListener(listener)
    }

    override fun setConnectChangeListener(connectChangeListener: ConnectChangeListener?) {
        mDelegate.setConnectChangeListener(connectChangeListener)
    }

    override fun ping(callback: (Boolean) -> Unit) {
        mDelegate.ping(callback)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    fun executeTask(callback: (Boolean) -> Unit) {
        thread {
            logger.info(tag, "开始执行任务")
            Thread.sleep(10000)
            logger.info(tag, "任务执行完毕")
            mainHandler.post() {
                callback.invoke(true)
            }

        }
    }

}