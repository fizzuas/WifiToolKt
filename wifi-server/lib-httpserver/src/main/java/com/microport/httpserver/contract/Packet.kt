package com.microport.httpserver.contract

import com.microport.httpserver.contract.content.PING_CONTENT
import com.microport.httpserver.exception.DecodePackageDataException
import com.microport.httpserver.util.CrcUtil
import com.microport.httpserver.util.ByteUtils
import java.text.ParseException

/**
 * 协议对应实体
 * 协议：head(1byte) + length(4byte 小端  cmd+content)+cmd(1byte)+content(变长，可null)+CCKK(1byte，由cmd和content计算 )
 *
 * @author ouyx
 * @date 2022/9/13 14:44
 */
const val HEAD = 0xAA.toByte()
const val FRAME_MAX_LENGTH = 10 * 1024 * 1024


class Packet(val cmd: Byte, val content: ByteArray? = null) {
    /**
     * [Packet]  编码成ByteArray
     */
    fun encode(): ByteArray {
        val headData = mutableListOf<Byte>().also { it.add(HEAD) }

        val sendData = ByteUtils.byteMerger(byteArrayOf(cmd), content).toList()

        val length = ByteUtils.int2ByteArrayLittle(sendData.size)
        val lengthData = byteArrayOf(length[0], length[1], length[2], length[3]).toList()

        val crcedData = mutableListOf<Byte>()

        crcedData.addAll(sendData)
        val crcData = CrcUtil.calcCrc(crcedData)

        val allData = mutableListOf<Byte>()
        allData.addAll(headData)
        allData.addAll(lengthData)
        allData.addAll(sendData)
        allData.add(crcData)

        return allData.toByteArray()
    }

    override fun toString(): String {
        return "(cmd=${ByteUtils.byte2HexString(cmd)}, content=${ByteUtils.byteArray2HexString(content)})"
    }

    companion object {
        /**
         *  ByteArray 解码成 [Packet]
         */
        fun decode(data: ByteArray): Packet {
            if (data[0] != HEAD) {
                throw ParseException("响应头不是$HEAD", -1)
            }
            val length = ByteUtils.byteArrayLittle2Int(data.sliceArray(1..4))
            val cmd = data[5]
            //length = cmd(1) +content(变长)
            val contentStart = 6
            val contentLength = length - 1
            val content = if (contentLength > 0) {
                data.sliceArray(contentStart until contentStart + contentLength)
            } else null

            if (data.size != length + 6) {
                throw DecodePackageDataException("Input data cannot decode to PackageData because of need ${length + 6} but get ${data.size}")
            }

            val sendData = ByteUtils.byteMerger(byteArrayOf(cmd), content)
            val cckk = ByteUtils.byte2HexString(data.last())
            if (ByteUtils.calculateCC(sendData) != cckk) {
                throw DecodePackageDataException("Input data cannot decode to PackageData because of CCKK")
            }
            return Packet(cmd, content)
        }

        fun getPingPacket(): Packet {
            return Packet(CMD.PING.value, PING_CONTENT)
        }
    }
}

