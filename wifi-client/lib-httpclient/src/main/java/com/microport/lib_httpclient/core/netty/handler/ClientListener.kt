package com.microport.lib_httpclient.core.netty.handler

import com.microport.lib_httpclient.exception.DecodePackageDataException
import io.netty.channel.Channel


/**
 *
 * @author ouyx
 * @date 2022/9/16 14:26
 */
interface ClientListener<T> {

    /**
     *  收到服务端消息
     */
    fun onMsgReceive(msg: T, channelId: String)


    /**
     * 与服务端建立连接
     */
    fun onChannelConnect(channel: Channel)

    /**
     * 与服务端断开连接
     */
    fun onChannelDisConnect(channel: Channel)

    /**
     *
     */
    fun onErrorCaught(error: DecodePackageDataException)


}