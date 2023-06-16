package com.microport.lib_httpclient.core.socket.transport

import com.client.util.ByteUtils
import com.microport.lib_httpclient.util.CrcUtil.calcCrc
import com.microport.wifi_client.util.log.DefaultLogger


class CommandStateMachine private constructor() {
    private val logger:DefaultLogger=DefaultLogger()

    private val tag = "State Machine"

    private val mCmdLength = 1

    private var mLengthByte1: Byte = 0
    private var mLengthByte2: Byte = 0
    private var mLengthByte3: Byte = 0

    //mLength = cmd(1 byte)+ content
    private var mLength = 0

    private var mCurrentCmd: Byte = 0

    private val mReceiveContent = mutableListOf<Byte>()
    private var mReceiveCRCData: Byte = 0
    private val mReceiveCrcedData = mutableListOf<Byte>()


    private var mReceiveDataParseListener: OnDataParseListener? = null


    enum class ReceiveDataState { HEADER1, LENGTH1, LENGTH2, LENGTH3, LENGTH4, COMMAND, DATA, CRC }
    enum class ParseReceiveDataState { PARSE_DATA_SUCCESSFULLY, PARSE_DATA_CONTINUE, PARSE_DATA_FAILED }

    private var mReceiveState = ReceiveDataState.HEADER1


    companion object {
        @Volatile
        private var INSTANCE: CommandStateMachine? = null

        fun getInstance(): CommandStateMachine {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = CommandStateMachine()
                    }
                }
            }
            return INSTANCE!!
        }

        const val HEAD = 0XAA.toByte()
    }

    private fun reset() {
        mReceiveState = ReceiveDataState.HEADER1

        mLengthByte1 = 0
        mLengthByte2 = 0
        mLengthByte3 = 0
        mLength = 0

        mCurrentCmd = 0

        mReceiveContent.clear()
        mReceiveCRCData = 0

        mReceiveCrcedData.clear()
    }

    private fun parseReceiveData(b: Byte, isDebug: Boolean): ParseReceiveDataState {
        return when (mReceiveState) {
            ReceiveDataState.HEADER1 -> if (b == HEAD) {
                if (isDebug) logger.info(tag, "HEADER1  ${ByteUtils.byte2HexString(b)}")
                mReceiveState = ReceiveDataState.LENGTH1
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            } else {
                ParseReceiveDataState.PARSE_DATA_FAILED
            }
            ReceiveDataState.LENGTH1 -> {
                if (isDebug) logger.info(tag, "LENGTH1 ${ByteUtils.byte2HexString(b)}")
                mLengthByte1 = b
                mReceiveState = ReceiveDataState.LENGTH2
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH2 -> {
                if (isDebug) logger.info(tag, "LENGTH2 ${ByteUtils.byte2HexString(b)}")
                mLengthByte2 = b
                mReceiveState = ReceiveDataState.LENGTH3
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH3 -> {
                if (isDebug) logger.info(tag, "LENGTH3 ${ByteUtils.byte2HexString(b)}")
                mLengthByte3 = b
                mReceiveState = ReceiveDataState.LENGTH4
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH4 -> {
                mLength = ByteUtils.byteArrayLittle2Int(byteArrayOf(mLengthByte1, mLengthByte2, mLengthByte3, b))
                if (isDebug) logger.info(tag, "LENGTH4 ${ByteUtils.byte2HexString(b)}" + ", 长度=" + mLength)
                if (mLength == 0) {
                    ParseReceiveDataState.PARSE_DATA_FAILED
                } else {
                    mReceiveState = ReceiveDataState.COMMAND
                    ParseReceiveDataState.PARSE_DATA_CONTINUE
                }
            }
            ReceiveDataState.COMMAND -> {
                if (isDebug) logger.info(tag, "COMMAND ${ByteUtils.byte2HexString(b)}")
                mReceiveCrcedData.add(b)
                mCurrentCmd = b
                mReceiveState = ReceiveDataState.DATA
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }

            ReceiveDataState.DATA -> {
                if (isDebug) logger.info(tag, "DATA  ${ByteUtils.byte2HexString(b)}")
                mReceiveCrcedData.add(b)
                mReceiveContent.add(b)
                if (mLength == mCmdLength + mReceiveContent.size) {
                    mReceiveState = ReceiveDataState.CRC
                }
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }

            ReceiveDataState.CRC -> {
                if (isDebug) logger.info(tag, "CRC ${ByteUtils.byte2HexString(b)}")
                mReceiveCRCData = b
                if (checkCrc(isDebug)) ParseReceiveDataState.PARSE_DATA_SUCCESSFULLY else ParseReceiveDataState.PARSE_DATA_FAILED
            }
        }
    }

    private fun checkCrc(isDebug: Boolean): Boolean {
        if (mReceiveCrcedData.isEmpty()) {
            logger.error(tag, "CRC 数据异常")
            return false
        }
        val calResult = calcCrc(mReceiveCrcedData)

        if (isDebug) {
            logger.info(tag, "被校验的CRCdata mReceiveCRCEDData=" + ByteUtils.byteArray2HexString(mReceiveCrcedData.toByteArray()))
            logger.info(tag, "计算得到CRC数据=" + ByteUtils.byte2HexString(calResult))
        }

//        if (calResult == mReceiveCRCData) {
//            return true
//        }
        return true
    }

    interface OnDataParseListener {
        fun onDataParseSuc(cmd: Byte, data: ByteArray)
        fun onDataParseFail(state: ReceiveDataState)
    }

    fun registerReceiveDataListener(parseListener: OnDataParseListener) { //view必须执行监听
        this.mReceiveDataParseListener = parseListener
    }

    fun unregisterReceiveDataListener() {
        mReceiveDataParseListener = null
        reset()
    }

    @Synchronized
    fun parseData(receivedData: ByteArray) {
        for (i in receivedData.indices) {
            addData(receivedData[i])
        }
    }

    private fun addData(b: Byte) {
        when (parseReceiveData(b, false)) {
            ParseReceiveDataState.PARSE_DATA_SUCCESSFULLY -> if (mReceiveDataParseListener != null) {
                mReceiveDataParseListener?.onDataParseSuc(
                    cmd = mCurrentCmd, data = mReceiveContent.toByteArray()
                )
                reset()
            }
            ParseReceiveDataState.PARSE_DATA_FAILED -> if (mReceiveDataParseListener != null) {
                logger.error(tag, "解析失败${mReceiveState.name}")
                mReceiveDataParseListener?.onDataParseFail(mReceiveState)
                reset()
            }
            ParseReceiveDataState.PARSE_DATA_CONTINUE -> Unit
        }
    }
}