/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.lib_httpclient.core.netty

import io.netty.channel.Channel
import java.net.InetSocketAddress


/**
 * [io.netty.channel.Channel] 扩展
 *
 * @author ouyx
 * @date 2023年04月20日 11时20分
 */

fun Channel.getShortId(): String = this.id().asShortText()

fun Channel.getLocalIP(): String? {
    val socketAddress = this.localAddress() as InetSocketAddress
    return socketAddress.address.hostAddress
}

fun Channel.getServerIP(): String? {
    val socketAddress = (this.remoteAddress() as InetSocketAddress)
    return socketAddress.address.hostAddress
}