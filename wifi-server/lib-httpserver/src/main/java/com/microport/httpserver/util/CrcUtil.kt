package com.microport.httpserver.util

/**
 *@Author oyx
 *@date 2021/7/6 18:55
 *@description
 */
object CrcUtil {
    fun calcCrc16(data: List<Byte>): ByteArray {
        val byteArray = ByteArray(2)
        var crc = 0 and 0xffff
        for (byte in data) {
            crc = (((crc shr 8) or (crc shl 8))) and 0xffff
            crc = (crc xor (byte.toInt() and 0xff)) and 0xffff
            crc = (crc xor ((crc and 0xff) shr 4)) and 0xffff
            crc = (crc xor (crc shl 12)) and 0xffff
            crc = (crc xor ((crc and 0xff) shl 5)) and 0xffff
        }
        byteArray[0] = ((crc shr 8) and 0xff).toByte()
        byteArray[1] = (crc and 0xff).toByte()
        return byteArray
    }


    fun calcCrc(data: List<Byte>): Byte {
        var cc: Byte = 0x00
        for (b in data) {
            cc = (cc + b).toByte()
        }
        return cc
    }


}