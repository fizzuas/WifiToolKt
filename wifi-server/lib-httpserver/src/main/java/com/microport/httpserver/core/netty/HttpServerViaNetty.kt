/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.core.netty

import android.os.Handler
import android.os.Looper
import com.microport.httpserver.contract.FRAME_MAX_LENGTH
import com.microport.httpserver.contract.Packet
import com.microport.httpserver.exception.DecodePackageDataException
import com.microport.httpserver.facade.interfaces.ConnectChangeListener
import com.microport.httpserver.facade.interfaces.IHttpServer
import com.microport.httpserver.facade.interfaces.PackageReceivedListener
import com.microport.httpserver.core.netty.decode.ContractDecoder
import com.microport.httpserver.core.netty.handler.NettyServerListener
import com.microport.httpserver.core.netty.handler.ServerHandler
import com.microport.httpserver.facade.interfaces.StartServerCallback
import com.microport.httpserver.util.ByteUtils
import com.microport.httpserver.util.log.DefaultLogger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.net.InetSocketAddress
import kotlin.concurrent.thread


/**
 * 基于Netty实现的HttpServer
 *
 * @author ouyx
 * @date 2023年03月21日 10时57分
 */
internal class HttpServerViaNetty : IHttpServer, NettyServerListener<Packet> {
    private val tag = HttpServerViaNetty::class.java.simpleName
    private val log = DefaultLogger()

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mConnectStateChangeListener: ConnectChangeListener? = null
    private var mPackageReceiveListener: PackageReceivedListener? = null
    private val mActiveChannels = mutableListOf<Channel>()

    @Volatile
    private var isStart = false

    override fun startServer(port: Int, callback: StartServerCallback?) {
        var callbackTemp = callback
        if (isStart) {
            callbackTemp?.onFail("Server is already running！")
            callbackTemp = null
            return
        }
        thread(name = "THREAD-NETTY-SERVER", start = true) {
            isStart = true
            bossGroup = NioEventLoopGroup(1)
            workerGroup = NioEventLoopGroup()
            try {
                val b = ServerBootstrap()
                b.group(bossGroup, workerGroup).channel(NioServerSocketChannel::class.java).localAddress(InetSocketAddress(port))
                    .childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true).childHandler(object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(ContractDecoder(maxFrameLength = FRAME_MAX_LENGTH))
                            ch.pipeline().addLast(ServerHandler(this@HttpServerViaNetty))
                        }
                    })
                val channelFuture = b.bind().sync()
                log.info(tag, "Server started and listen on $port ")
                callbackTemp?.onSuccess()
                callbackTemp = null

                channelFuture.channel().closeFuture().sync()
            } catch (e: Exception) {
                e.printStackTrace()
                callbackTemp?.onFail(e.message)
                callbackTemp = null
            } finally {
                log.info(tag, "Shut down server!")
                workerGroup?.shutdownGracefully()
                bossGroup?.shutdownGracefully()
                isStart = false
            }
        }
    }

    override fun sendPackage(aPacket: Packet, callback: (Boolean) -> Unit) {
        if (mActiveChannels.none { it.isActive }) {
            log.error(tag, "No client connected")
            mainHandler.post { callback.invoke(false) }
            return
        }
        val channel = mActiveChannels.first { it.isActive }
        val bytes = aPacket.encode()
        val buf = Unpooled.copiedBuffer(bytes as ByteArray?)
        channel.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
            if (channelFuture.isSuccess) {
                mainHandler.post {
                    log.info(tag, ">>>send success $aPacket")
                    callback.invoke(true) }
            } else {
                mainHandler.post {
                    log.error(tag, ">>>send fail= $aPacket")
                    callback.invoke(false) }
            }
        })
    }

    override fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit) {
        if (mActiveChannels.none { it.isActive }) {
            log.error(tag, "No client connected")
            mainHandler.post { callback.invoke(false) }
            return
        }
        val channel = mActiveChannels.first { it.isActive }
        val buf = Unpooled.copiedBuffer(data as ByteArray?)
        channel.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
            if (channelFuture.isSuccess) {
                log.info(tag, ">>>send success ${ByteUtils.byteArray2HexString(data)}")
                mainHandler.post { callback.invoke(true) }
            } else {
                log.error(tag, ">>>send fail= ${ByteUtils.byteArray2HexString(data)}")
                mainHandler.post { callback.invoke(false) }
            }
        })
    }

    override fun setReceivePackageListener(listener: PackageReceivedListener?) {
        mPackageReceiveListener = listener
    }


    override fun setConnectChangeListener(connectChangeListener: ConnectChangeListener?) {
        mConnectStateChangeListener = connectChangeListener
    }


    override fun ping(callback: (Boolean) -> Unit) {
        if (mActiveChannels.none { it.isActive }) {
            mainHandler.post {
                callback.invoke(false)
            }
            return
        }
        val channel = mActiveChannels.first { it.isActive }
        val bytes = Packet.getPingPacket().encode()
        val buf = Unpooled.copiedBuffer(bytes as ByteArray?)
        channel.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
            mainHandler.post {
                if (channelFuture.isSuccess) {
                    callback.invoke(true)
                    log.info(tag, ">>>Send data = ${Packet.getPingPacket()} success on channelId =${getId(channel)}")
                } else {
                    callback.invoke(false)
                    log.error(tag, ">>>Send data = ${Packet.getPingPacket()} fail on channelId =${getId(channel)}")
                }
            }
        })
    }


    override fun onMsgReceive(msg: Packet, channelId: String) {
        log.info(tag, "<<<Receive data = $msg on channelId =$channelId")
        mainHandler.post {
            mPackageReceiveListener?.onReceived(cmd = msg.cmd, data = msg.content)
        }
    }

    override fun onChannelConnect(channel: Channel) {
        val channelId = getId(channel)
        val clientIp = getClientIp(channel)
        log.info(tag, "ChannelId=${channelId}  clientIP=${clientIp} 连接上了 ")
        mActiveChannels.add(channel)
        mainHandler.post {
            mConnectStateChangeListener?.onConnected(channelId, clientIp)
        }
    }

    override fun onChannelDisConnect(channel: Channel) {
        val channelId = getId(channel)
        val clientIp = getClientIp(channel)
        log.info(tag, "ChannelId=${channelId} clientIP=${clientIp} 断开了连接")
        mActiveChannels.removeIf { it == channel }
        mainHandler.post {
            mConnectStateChangeListener?.onDisConnect(channelId, clientIp)
        }
    }

    override fun onErrorCaught(error: DecodePackageDataException) {
    }

    private fun getId(channel: Channel?) = if (channel == null) "null" else channel.id().asShortText()

    private fun getClientIp(channel: Channel): String? {
        val ipSocket = (channel.remoteAddress() as InetSocketAddress)
        return ipSocket.address.hostAddress
    }
}