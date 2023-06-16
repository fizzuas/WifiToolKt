package com.microport.httpserver.core.socket.transport

import com.microport.httpserver.facade.interfaces.IDataReceive
import com.microport.httpserver.util.log.DefaultLogger
import java.io.IOException
import java.io.InputStream

/**
 * @author ouyx
 * @date 2022/11/24 17:35
 */
class UsbReceivedThread(private var mInputStream: InputStream?, private val dataReceive: IDataReceive, name: String) : Thread(name) {
    private val tag = "UsbReceivedThread"
    private val log = DefaultLogger()
    private val mReceivedBuffer: ByteArray = ByteArray(1024)
    override fun run() {
        super.run()
        while (!interrupted()) {
            try {
                if (null == mInputStream) {
                    return
                }
                val size = mInputStream!!.read(mReceivedBuffer)
                if (size <= 0) {
                    return
                }
                val receivedBytes = ByteArray(size)
                System.arraycopy(mReceivedBuffer, 0, receivedBytes, 0, size)
                dataReceive.onDataReceived(receivedBytes)
            } catch (e: IOException) {
                log.error(tag, "USB Received thread catch exception" + if (e.message == null) "" else e.message!!)
            }
        }
    }
    fun release() {
        interrupt()
        if (null != mInputStream) {
            try {
                mInputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mInputStream = null
        }
    }

    companion object {
        const val USB_RECEIVED_THREAD_NAME_PREFIX = "REC_THREAD"
    }
}