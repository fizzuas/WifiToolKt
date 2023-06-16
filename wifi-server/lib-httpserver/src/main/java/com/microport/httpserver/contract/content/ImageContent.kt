package com.microport.httpserver.contract.content

import com.microport.httpserver.exception.DecodeImgPackageException
import com.microport.httpserver.util.ByteUtils

/**
 * [com.microport.httpserver.contract.Packet]中 content类型 为图像包 所对应的实体
 *
 * @author ouyx
 * @date 2022/11/30 16:10
 *
 */
class ImageContent( val index: Long, var ts: Long = 0,  val imgBytes: ByteArray) {
    fun encode(): ByteArray {
        val byteList = mutableListOf<Byte>()
        byteList.addAll(ByteUtils.long2ByteArrayLittle(index).toList())
        byteList.addAll(ByteUtils.long2ByteArrayLittle(ts).toList())
        byteList.addAll(imgBytes.toList())
        return byteList.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): ImageContent {
            if (bytes.size < 16) {
                throw DecodeImgPackageException("ImgPackage at least has 16 bytes,because of at least has 8 bytes index and 8 bytes length!")
            }
            val indexByteArray = ByteArray(8)
            val tsByteArray = ByteArray(8)
            System.arraycopy(bytes, 0, indexByteArray, 0, 8)
            System.arraycopy(bytes, 8, tsByteArray, 0, 8)
            val index = ByteUtils.byteArrayLittle2Long(indexByteArray)
            val ts = ByteUtils.byteArrayLittle2Long(tsByteArray)
            val imgBytes = ByteArray(bytes.size - 16)
            System.arraycopy(bytes, 16, imgBytes, 0, imgBytes.size)
            return ImageContent(index, ts, imgBytes)
        }
    }

    override fun toString(): String {
        return "{index=${index},ts=$ts,imgBytes.size=${imgBytes.size}}"
    }
}