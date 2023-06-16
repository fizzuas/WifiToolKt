package com.microport.httpserver.core.netty.handler


import com.microport.httpserver.contract.Packet
import com.microport.httpserver.util.log.DefaultLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

/**
 * @author ouyx
 * @date 2022/9/13 14:30
 */
class ServerHandler(private val listener: NettyServerListener<Packet>?) : SimpleChannelInboundHandler<Any>() {
    private val tag = ServerHandler::class.java.simpleName
    private val log = DefaultLogger()

    /**
     * 客户端收到消息
     */
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        val buf = msg as ByteBuf
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        val packet = Packet.decode(bytes)
        if (ctx != null) {
            listener?.onMsgReceive(packet, getId(ctx.channel()))
        }
    }

    /**
     * 客户端上线
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        listener?.onChannelConnect(ctx.channel())
    }

    /**
     * 客户端下线
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        log.info(tag, "channelInactive")
        listener?.onChannelDisConnect(ctx.channel())
    }


    private fun getId(channel: Channel?) = if (channel == null) "null" else channel.id().asShortText()
}