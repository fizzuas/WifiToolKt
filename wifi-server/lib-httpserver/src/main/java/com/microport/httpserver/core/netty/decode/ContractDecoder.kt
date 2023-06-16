/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.core.netty.decode

import com.microport.httpserver.util.log.DefaultLogger
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import java.nio.ByteOrder


/**
 * 查看 [com.microport.httpserver.contract.Packet]
 *协议：head(1byte) + length(4byte 小端  cmd+content)+cmd(1byte)+content(变长，可null)+CCKK(1byte，由cmd和content计算 )
 *
 * @author admin
 * @date 2023年03月22日 14时50分
 */
class ContractDecoder(byteOrder: ByteOrder? = ByteOrder.LITTLE_ENDIAN,
                      maxFrameLength: Int,
                      lengthFieldOffset: Int = 1,
                      lengthFieldLength: Int = 4,
                      lengthAdjustment: Int = 1,
                      initialBytesToStrip: Int = 0,
                      failFast: Boolean = false) :
    LengthFieldBasedFrameDecoder(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast) {
    private val tag = ContractDecoder::class.java.simpleName
    private val log = DefaultLogger()
    override fun getUnadjustedFrameLength(buf: ByteBuf?, offset: Int, length: Int, order: ByteOrder?): Long {
//        val lengthBytes = ByteArray(length) { 0.toByte() }
//        buf!!.getBytes(offset, lengthBytes)
//        log.info(tag, "ContractDecoder$getUnadjustedFrameLength   (cmd+content)长度 = ${ByteUtils.byteArrayLittle2Int(lengthBytes)}")
        return super.getUnadjustedFrameLength(buf, offset, length, order)
    }
}