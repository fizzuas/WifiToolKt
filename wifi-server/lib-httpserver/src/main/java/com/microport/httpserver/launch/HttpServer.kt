/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.launch

import com.microport.httpserver.contract.Packet
import com.microport.httpserver.core.netty.HttpServerViaNetty
import com.microport.httpserver.facade.interfaces.ConnectChangeListener
import com.microport.httpserver.facade.interfaces.IHttpServer
import com.microport.httpserver.facade.interfaces.PackageReceivedListener
import com.microport.httpserver.facade.interfaces.StartServerCallback


/**
 *  HttpServer
 *
 * @author ouyx
 * @date 2023年04月14日 15时23分
 */
class HttpServer private constructor() : IHttpServer {
    private var mDelegate: IHttpServer = HttpServerViaNetty()

    companion object {
        @Volatile
        private var instance: HttpServer? = null
        fun getInstance(): HttpServer = instance ?: synchronized(this) {
            instance ?: HttpServer().also { instance = it }
        }
    }

    fun setDelegate(httpServer: IHttpServer?) {
        mDelegate = httpServer ?: mDelegate
    }

    override fun startServer(port: Int, callback: StartServerCallback?) {
        mDelegate.startServer(port, callback)
    }

    override fun sendPackage(aPacket: Packet, callback: (Boolean) -> Unit) {
        mDelegate.sendPackage(aPacket,callback)
    }

    override fun sendBytes(data: ByteArray,callback: (Boolean) -> Unit) {
        mDelegate.sendBytes(data,callback)
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
}