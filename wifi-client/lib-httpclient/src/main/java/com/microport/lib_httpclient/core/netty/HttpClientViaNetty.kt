/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.lib_httpclient.core.netty

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.client.util.ByteUtils
import com.microport.lib_httpclient.contract.FRAME_MAX_LENGTH
import com.microport.lib_httpclient.contract.Packet
import com.microport.lib_httpclient.core.netty.decode.ContractDecoder
import com.microport.lib_httpclient.core.netty.handler.ClientHandler
import com.microport.lib_httpclient.core.netty.handler.ClientListener
import com.microport.lib_httpclient.exception.DecodePackageDataException
import com.microport.lib_httpclient.facade.*
import com.microport.wifi_client.util.log.DefaultLogger
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import kotlin.concurrent.thread


/**
 * HttpClientViaNetty
 *
 * @author ouyx
 * @date 2023年04月19日 10时05分
 */
class HttpClientViaNetty : IHttpClient, ClientListener<Packet> {
    private val tag = HttpClientViaNetty::class.java.simpleName
    private val log = DefaultLogger()

    private val handlerThread = HandlerThread("thread_http_client")
    private val mWorkHandler: Handler

    init {
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mConnectStateChangeListener: ConnectChangeListener? = null
    private var mPackageReceiveListener: PackageReceivedListener? = null
    private var mActiveChannel: Channel? = null

    @Volatile
    private var isStart = false


    override fun connectSever(host: String, port: Int, callback: ConnectClientCallback?) {
        var callbackTemp = callback
        if (isStart) {
            callbackTemp?.onFail("Server is already running！")
            callbackTemp = null
            return
        }
        thread(name = "thread-httpclient", start = true) {
            isStart = true
            val group: EventLoopGroup = NioEventLoopGroup()
            try {
                val b = Bootstrap()
                b.group(group)
                    .channel(NioSocketChannel::class.java)
                    .handler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(ContractDecoder(maxFrameLength = FRAME_MAX_LENGTH))
                            ch.pipeline().addLast(ClientHandler(this@HttpClientViaNetty))
                        }
                    })

                // Start the client.
                val f: ChannelFuture = b.connect(host, port).sync()

                log.info(tag, "Client connect server successfully")
                callbackTemp?.onSuccess()
                callbackTemp = null

                // Wait until the connection is closed.
                f.channel().closeFuture().sync()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                callbackTemp?.onFail(e.message)
                callbackTemp = null
            } finally {
                // Shut down the event loop to terminate all threads.
                log.info(tag, "Shut down server!")
                group.shutdownGracefully()
                isStart = false
            }
        }
    }

    override fun disConnect() {
        if (mActiveChannel == null) {
            return
        }
        mActiveChannel!!.disconnect().addListener(ChannelFutureListener { channelFuture ->
            if (channelFuture.isSuccess) {
                log.info(tag, "断开成功")
            } else {
                log.error(tag, "断开失败")
            }
        })
    }


    override fun sendPackage(packet: Packet, callback: (Boolean) -> Unit) {
        if (mActiveChannel != null) {
            val bytes = packet.encode()
            val buf = Unpooled.copiedBuffer(bytes as ByteArray?)
            mActiveChannel!!.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
                if (channelFuture.isSuccess) {
                    mainHandler.post {
                        log.info(tag, ">>>send success $packet")
                        callback.invoke(true)
                    }
                } else {
                    mainHandler.post {
                        log.error(tag, ">>>send fail= $packet")
                        callback.invoke(false)
                    }
                }
            })
        } else {
            mainHandler.post { callback.invoke(false) }
        }
    }

    override fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit) {
        if (mActiveChannel == null) {
            log.error(tag, "No client connected")
            mainHandler.post { callback.invoke(false) }
            return
        }

        val buf = Unpooled.copiedBuffer(data as ByteArray?)
        mActiveChannel!!.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
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
        if (mActiveChannel == null) {
            mainHandler.post {
                callback.invoke(false)
            }
            return
        }
        val bytes = Packet.getPingPacket().encode()
        val buf = Unpooled.copiedBuffer(bytes as ByteArray?)
        mActiveChannel!!.writeAndFlush(buf).addListener(ChannelFutureListener { channelFuture ->
            mainHandler.post {
                if (channelFuture.isSuccess) {
                    callback.invoke(true)
                    log.info(tag, ">>>Send data = ${Packet.getPingPacket()} success on channelId =${mActiveChannel?.getShortId()}")
                } else {
                    callback.invoke(false)
                    log.error(tag, ">>>Send data = ${Packet.getPingPacket()} fail on channelId =${mActiveChannel?.getShortId()}")
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
        val channelId = channel.getShortId()
        val localIP = channel.getLocalIP()
        val serverIP = channel.getServerIP()
        log.info(tag, "ChannelId=${channelId}  与serverIP=${serverIP} 连接上了 ")
        val connection = Connection(channelId, localIP, serverIP)
        mActiveChannel = channel
        mainHandler.post {
            mConnectStateChangeListener?.onConnected(connection)
        }
    }

    override fun onChannelDisConnect(channel: Channel) {
        val channelId = channel.getShortId()
        val localIP = channel.getLocalIP()
        val serverIP = channel.getServerIP()
        log.info(tag, "ChannelId=${channelId} 与serverIP=${serverIP} 断开了连接")
        val connection = Connection(channelId, localIP, serverIP)
        mActiveChannel = null
        mainHandler.post {
            mConnectStateChangeListener?.onDisConnect(connection)
        }
    }

    override fun onErrorCaught(error: DecodePackageDataException) {
    }


}