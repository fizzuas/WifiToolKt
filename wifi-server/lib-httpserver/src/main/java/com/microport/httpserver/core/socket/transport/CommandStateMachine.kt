package com.microport.httpserver.core.socket.transport

import com.microport.httpserver.contract.HEAD
import com.microport.httpserver.util.ByteUtils
import com.microport.httpserver.util.CrcUtil.calcCrc
import com.microport.httpserver.util.log.DefaultLogger
import com.microport.httpserver.util.log.ILogger


/**
 *  状态机组包工具
 *
 *  @author ouyx
 *  @date 2023年3月24日
 *
 */
class CommandStateMachine private constructor() {

    private val tag = "State Machine"
    private val log: ILogger = DefaultLogger(isShowLog = false)

    private val mCmdLength = 1

    private var mLengthByte1: Byte = 0
    private var mLengthByte2: Byte = 0
    private var mLengthByte3: Byte = 0

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

    private fun parseReceiveData(b: Byte): ParseReceiveDataState {
        return when (mReceiveState) {
            ReceiveDataState.HEADER1 -> if (b == HEAD) {
                log.info(tag, "HEADER1  ${ByteUtils.byte2HexString(b)}")
                mReceiveState = ReceiveDataState.LENGTH1
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            } else {
                ParseReceiveDataState.PARSE_DATA_FAILED
            }
            ReceiveDataState.LENGTH1 -> {
                log.info(tag, "LENGTH1 ${ByteUtils.byte2HexString(b)}")
                mLengthByte1 = b
                mReceiveState = ReceiveDataState.LENGTH2
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH2 -> {
                log.info(tag, "LENGTH2 ${ByteUtils.byte2HexString(b)}")
                mLengthByte2 = b
                mReceiveState = ReceiveDataState.LENGTH3
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH3 -> {
                log.info(tag, "LENGTH3 ${ByteUtils.byte2HexString(b)}")
                mLengthByte3 = b
                mReceiveState = ReceiveDataState.LENGTH4
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }
            ReceiveDataState.LENGTH4 -> {
                mLength = ByteUtils.byteArrayLittle2Int(byteArrayOf(mLengthByte1, mLengthByte2, mLengthByte3, b))
                log.info(tag, "LENGTH4 ${ByteUtils.byte2HexString(b)}" + ", 长度=" + mLength)
                if (mLength == 0) {
                    ParseReceiveDataState.PARSE_DATA_FAILED
                } else {
                    mReceiveState = ReceiveDataState.COMMAND
                    ParseReceiveDataState.PARSE_DATA_CONTINUE
                }
            }
            ReceiveDataState.COMMAND -> {
                log.info(tag, "COMMAND ${ByteUtils.byte2HexString(b)}")
                mReceiveCrcedData.add(b)
                mCurrentCmd = b
                mReceiveState = ReceiveDataState.DATA
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }

            ReceiveDataState.DATA -> {
                log.info(tag, "DATA  ${ByteUtils.byte2HexString(b)}")
                mReceiveCrcedData.add(b)
                mReceiveContent.add(b)
                if (mLength == mCmdLength + mReceiveContent.size) {
                    mReceiveState = ReceiveDataState.CRC
                }
                ParseReceiveDataState.PARSE_DATA_CONTINUE
            }

            ReceiveDataState.CRC -> {
                log.info(tag, "CRC ${ByteUtils.byte2HexString(b)}")
                mReceiveCRCData = b
                //忽略校验crc
                ParseReceiveDataState.PARSE_DATA_SUCCESSFULLY
            }
        }
    }


    private fun addData(b: Byte) {
        when (parseReceiveData(b)) {
            ParseReceiveDataState.PARSE_DATA_SUCCESSFULLY -> if (mReceiveDataParseListener != null) {
                mReceiveDataParseListener?.onDataParseSuc(cmd = mCurrentCmd, data = mReceiveContent)
                reset()
            }
            ParseReceiveDataState.PARSE_DATA_FAILED -> if (mReceiveDataParseListener != null) {
                log.error(tag, "解析失败${mReceiveState.name}")
                mReceiveDataParseListener?.onDataParseFail(mReceiveState)
                reset()
            }
            ParseReceiveDataState.PARSE_DATA_CONTINUE -> Unit
        }
    }

    private fun checkCrc(): Boolean {
        if (mReceiveCrcedData.isEmpty()) {
            log.error(tag, "CRC 数据异常")
            return false
        }
        val calResult = calcCrc(mReceiveCrcedData)
        if (calResult == mReceiveCRCData) {
            return true
        }
        return true
    }


    interface OnDataParseListener {
        fun onDataParseSuc(cmd: Byte, data: List<Byte>)
        fun onDataParseFail(state: ReceiveDataState)
    }

}