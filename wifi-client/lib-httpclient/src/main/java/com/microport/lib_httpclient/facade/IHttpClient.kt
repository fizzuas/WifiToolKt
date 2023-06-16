package com.microport.lib_httpclient.facade

import com.microport.lib_httpclient.constant.Constant


/**
 * IHttpServer
 *
 * @author ouyx
 * @date 2023年03月20日 18时23分
 */
interface IHttpClient {
    /**
     *  在指定端口port(默认时SERVER_DEFAULT_PORT 1995)连接 HttpServer
     */
    fun connectSever(host: String, port: Int = Constant.SERVER_DEFAULT_PORT, callback: ConnectClientCallback?)


    fun disConnect()

    /**
     * 发送 [com.microport.lib_httpclient.contract.Packet]
     */
    fun sendPackage(packet: com.microport.lib_httpclient.contract.Packet, callback: (Boolean) -> Unit)

    /**
     * 发送源数据
     */
    fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit)

    /**
     * 收到Client端 包回调，包结构[com.microport.lib_httpclient.contract.Packet]
     */
    fun setReceivePackageListener(listener: PackageReceivedListener?)

    /**
     * 设置 Client 连接状态发生变化 的监听器
     */
    fun setConnectChangeListener(connectChangeListener: ConnectChangeListener?)

    /**
     *  测试连接是否通
     *
     */
    fun ping(callback: (Boolean) -> Unit)
}