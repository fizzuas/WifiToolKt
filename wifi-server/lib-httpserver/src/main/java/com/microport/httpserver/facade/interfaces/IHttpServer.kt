package com.microport.httpserver.facade.interfaces

import com.microport.httpserver.constant.Constant
import com.microport.httpserver.contract.Packet


/**
 * IHttpServer
 *
 * @author ouyx
 * @date 2023年03月20日 18时23分
 */
interface IHttpServer {
    /**
     *  在指定端口port(默认时SERVER_DEFAULT_PORT 1995)启动服务
     */
    fun startServer(port: Int = Constant.SERVER_DEFAULT_PORT, callback: StartServerCallback? = null)

    /**
     * 发送PackageData
     */
    fun sendPackage(aPacket: Packet, callback: (Boolean) -> Unit)

    /**
     * 发送源数据
     */
    fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit)

    /**
     * 收到Client端 包回调，包结构[com.microport.httpserver.contract.Packet]
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