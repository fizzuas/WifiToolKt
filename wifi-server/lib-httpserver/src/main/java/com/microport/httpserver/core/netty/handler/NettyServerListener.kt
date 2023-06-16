package com.microport.httpserver.core.netty.handler

import com.microport.httpserver.exception.DecodePackageDataException
import io.netty.channel.Channel


/**
 *
 * @author ouyx
 * @date 2022/9/16 14:26
 */
interface NettyServerListener<T> {

    /**
     *  收到客户端消息
     */
    fun onMsgReceive(msg: T, channelId: String)


    /**
     * 与客户端建立连接
     */
    fun onChannelConnect(channel: Channel)

    /**
     * 与客户端断开连接
     */
    fun onChannelDisConnect(channel: Channel)

    /**
     *
     */
    fun onErrorCaught(error: DecodePackageDataException)


}