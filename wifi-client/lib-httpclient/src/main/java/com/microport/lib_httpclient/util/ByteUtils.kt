package com.client.util

import java.io.*
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.function.IntPredicate
import kotlin.math.ceil

class ByteUtils {
    /**
     * 获得指定文件的byte数组
     */
    private fun getBytes(filePath: String): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            val bos = ByteArrayOutputStream(1000)
            val b = ByteArray(1000)
            var n: Int
            while (fis.read(b).also { n = it } != -1) {
                bos.write(b, 0, n)
            }
            fis.close()
            bos.close()
            buffer = bos.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return buffer
    }

    companion object {
        private const val HEX = "0123456789ABCDEF"

        fun isHexStr(hexString: String): Boolean {
            return hexString.chars().allMatch { value -> HEX.contains(value.toChar(), true) }
        }

        /**
         * HEXString 转 HEX byte[]
         * HEXString每一个char代表半个字节
         */
        fun hexString2byteArray(hexString: String): ByteArray {
            require(isHexStr(hexString)) {
                "参数内容必须是HEX,当前传入参数内容是${hexString}"
            }
            require(hexString.length % 2 == 0) {
                "参数内容长度必须是偶数,当前传输参数长度=${hexString.length}"
            }
            val hexCharArray = hexString.toCharArray()
            val byteArray = ByteArray(ceil(hexString.length.toDouble() / 2).toInt())
            val temp = IntArray(2)
            var i = 0
            while (i < hexString.length) {
                for (j in 0..1) {
                    if (hexCharArray[i + j] in '0'..'9') {
                        temp[j] = hexCharArray[i + j] - '0'
                    } else if (hexCharArray[i + j] in 'A'..'F') {
                        temp[j] = hexCharArray[i + j] - 'A' + 10
                    } else if (hexCharArray[i + j] in 'a'..'f') {
                        temp[j] = hexCharArray[i + j] - 'a' + 10
                    }
                }
                temp[0] = temp[0] and 0xff shl 4
                temp[1] = temp[1] and 0xff
                byteArray[i / 2] = (temp[0] or temp[1]).toByte()
                i += 2
            }
            return byteArray
        }

        // 合并两个byte数组
        fun byteMerger(byte_1: ByteArray, byte_2: ByteArray?): ByteArray {
            if (byte_2 != null) {
                val byte_3 = ByteArray(byte_1.size + byte_2.size)
                System.arraycopy(byte_1, 0, byte_3, 0, byte_1.size)
                System.arraycopy(byte_2, 0, byte_3, byte_1.size, byte_2.size)
                return byte_3
            }
            return byte_1
        }

        fun byteArray2HexString(byteArray: ByteArray?): String {
            if (byteArray == null) return ""
            val result = StringBuffer(2 * byteArray.size)
            for (aByteArray in byteArray) {
                appendHex(result, aByteArray)
            }
            return result.toString()
        }

        fun byteArray2HexStringWithMargin(byteArray: ByteArray?): String {
            val content = byteArray2HexString(byteArray)
            val sb = StringBuilder()
            for (i in 0 until content.length) {
                sb.append(content[i])
                if ((i + 1) % 2 == 0) {
                    sb.append("  ")
                }
                if ((i + 1) % 20 == 0) {
                    sb.append("\n")
                }
            }
            return sb.toString()
        }

        fun byte2HexString(aByte: Byte): String {
            val result = StringBuffer(2)
            appendHex(result, aByte)
            return result.toString()
        }

        fun byteListAddBytes(byteList: MutableList<Byte?>, bytes: ByteArray): List<Byte?> {
            for (b in bytes) {
                byteList.add(b)
            }
            return byteList
        }

        fun byteList2HexString(byteList: List<Byte>?): String {
            if (byteList != null) {
                val bytes = byteList2ByteArray(byteList)
                return byteArray2HexString(bytes)
            }
            return "  "
        }

        fun byteList2ByteArray(byteList: List<Byte>): ByteArray {
            val bytes = ByteArray(byteList.size)
            for (i in byteList.indices) {
                bytes[i] = byteList[i]
            }
            return bytes
        }

        /**
         * 将byte[]中的部分转换成字符串
         *
         * @param source     源数组
         * @param startIndex 起始位置
         * @param length     String长度
         * @return 转换的部分字符串
         */
        fun byteArray2String(source: ByteArray?, startIndex: Int, length: Int): String {
            val resultArray = ByteArray(length)
            System.arraycopy(source, startIndex, resultArray, 0, length)
            return String(resultArray)
        }

        fun int2ByteArrayLittle(aInt: Int): ByteArray {
            val b = ByteArray(4)
            b[0] = (aInt and 0xff).toByte()
            b[1] = (aInt shr 8 and 0xff).toByte()
            b[2] = (aInt shr 16 and 0xff).toByte()
            b[3] = (aInt shr 24 and 0xff).toByte()
            return b
        }

        fun int2ByteArrayBig(aInt: Int): ByteArray {
            val b = ByteArray(4)
            b[0] = (aInt shr 24 and 0xff).toByte()
            b[1] = (aInt shr 16 and 0xff).toByte()
            b[2] = (aInt shr 8 and 0xff).toByte()
            b[3] = (aInt and 0xff).toByte()
            return b
        }


        fun byteArrayLittle2Int(bytes: ByteArray): Int {
            val a: Int = bytes[0].toInt() and 0xff
            val b: Int = bytes[1].toInt() and 0xff shl 8
            val c: Int = bytes[2].toInt() and 0xff shl 16
            val d: Int = bytes[3].toInt() and 0xff shl 24
            return a or b or c or d
        }


        fun byteArrayBig2Int(b: ByteArray): Int {
            val c: Int = b[0].toInt() and 0xff shl 8
            val a: Int = b[1].toInt() and 0xff
            return a or c
        }

        fun long2ByteArrayLittle(value: Long): ByteArray {
            val bytes = ByteArray(8)
            bytes[0] = (value and 0xff).toByte()
            bytes[1] = (value shr 8 and 0xff).toByte()
            bytes[2] = (value shr 16 and 0xff).toByte()
            bytes[3] = (value shr 24 and 0xff).toByte()
            bytes[4] = (value shr 32 and 0xff).toByte()
            bytes[5] = (value shr 40 and 0xff).toByte()
            bytes[6] = (value shr 48 and 0xff).toByte()
            bytes[7] = (value shr 56 and 0xff).toByte()
            return bytes
        }

        fun byteArrayLittle2Long(array: ByteArray): Long {
            val a = (array[0].toLong() and 0xff) shl 0
            val b = (array[1].toLong() and 0xff) shl 8
            val c = (array[2].toLong() and 0xff) shl 16
            val d = (array[3].toLong() and 0xff) shl 24
            val e = (array[4].toLong() and 0xff) shl 32
            val f = (array[5].toLong() and 0xff) shl 40
            val g = (array[6].toLong() and 0xff) shl 48
            val h = (array[7].toLong() and 0xff) shl 56
            return a or b or c or d or e or f or g or h
        }

        private fun appendHex(sb: StringBuffer, b: Byte) {
            sb.append(HEX[b.toInt() shr 4 and 0x0f]).append(HEX[b.toInt() and 0x0f])
        }

        // 将接收到的数据更易读
        fun addSpace(hex: String): String {
            val length = hex.length
            val builder = StringBuilder()
            var i = 0
            while (i < length) {

                // 长度为单数的情况
                if (i == length - 1) {
                    builder.append(hex.substring(i, i + 1))
                } else {
                    builder.append(hex.substring(i, i + 2))
                    builder.append(" ")
                }
                i += 2
            }
            return builder.toString().trim { it <= ' ' }
        }

        fun byteArray2ByteList(bytes: ByteArray): List<Byte> {
            val list: MutableList<Byte> = ArrayList()
            for (b in bytes) {
                list.add(b)
            }
            return list
        }

        fun hexString2ByteList(hex: String): List<Byte> {
            return byteArray2ByteList(hexString2byteArray(hex))
        }

        fun toHex(buf: ByteArray?): String {
            if (buf == null) return ""
            val result = StringBuffer(2 * buf.size)
            for (aBuf in buf) {
                appendHex(result, aBuf)
            }
            return result.toString()
        }

        fun toHex(buf: List<Byte>?): String {
            return byteList2HexString(buf)
        }

        fun calculateCC(data: ByteArray): String {
            var cc: Byte = 0x00
            for (b in data) {
                cc = (cc + b).toByte()
            }
            return byte2HexString(cc)
        }
    }
}