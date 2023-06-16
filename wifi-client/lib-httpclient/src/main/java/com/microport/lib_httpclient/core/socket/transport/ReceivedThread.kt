package com.microport.lib_httpclient.core.socket.transport

import java.io.IOException
import java.io.InputStream

/**
 * @author ouyx
 * @date 2022/11/24 17:35
 */
abstract class ReceivedThread(private var mInputStream: InputStream?) : Thread() {
    private val mReceivedBuffer: ByteArray = ByteArray(1024)
    override fun run() {
        super.run()
        while (!isInterrupted) {
            try {
                if (null == mInputStream) {
                    return
                }
                val size = mInputStream!!.read(mReceivedBuffer)
                if (0 >= size) {
                    return
                }
                val receivedBytes = ByteArray(size)
                System.arraycopy(mReceivedBuffer, 0, receivedBytes, 0, size)
                onDataReceived(receivedBytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    abstract fun onDataReceived(bytes: ByteArray)

    /**
     * 释放
     */
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
        private val TAG = ReceivedThread::class.java.simpleName
    }

}