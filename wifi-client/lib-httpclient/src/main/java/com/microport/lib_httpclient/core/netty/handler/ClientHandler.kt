/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.lib_httpclient.core.netty.handler

import com.microport.lib_httpclient.contract.Packet
import com.microport.lib_httpclient.core.netty.getShortId
import com.microport.wifi_client.util.log.DefaultLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler


/**
 *
 *
 * @author ouyx
 * @date 2023年04月19日 17时12分
 */
class ClientHandler(private val listener: ClientListener<Packet>?) : SimpleChannelInboundHandler<Any>() {
    private val tag = ClientHandler::class.java.simpleName
    private val log = DefaultLogger()

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        val buf = msg as ByteBuf
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        val packet = Packet.decode(bytes)
        if (ctx != null) {
            listener?.onMsgReceive(packet, ctx.channel().getShortId())
        }
    }

    /**
     * 与服务端建立连接回调
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        log.info(tag, "与服务端建立连接 $ctx")
        listener?.onChannelConnect(ctx.channel())
    }

    /**
     * 与服务端断开回调
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        log.info(tag, "与服务端断开 $ctx")
        listener?.onChannelDisConnect(ctx.channel())
    }


    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        super.exceptionCaught(ctx, cause)
        cause?.printStackTrace()
    }
}
