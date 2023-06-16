package com.microport.lib_httpclient.core.socket


import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.client.util.ByteUtils
import com.microport.lib_httpclient.contract.CMD
import com.microport.lib_httpclient.contract.Packet
import com.microport.lib_httpclient.core.socket.transport.CommandStateMachine
import com.microport.lib_httpclient.core.socket.transport.ReceivedThread
import com.microport.lib_httpclient.facade.ConnectChangeListener
import com.microport.lib_httpclient.facade.ConnectClientCallback
import com.microport.lib_httpclient.facade.IHttpClient
import com.microport.lib_httpclient.facade.PackageReceivedListener
import com.microport.wifi_client.util.log.DefaultLogger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.concurrent.thread

class HttpClientViaSocket : IHttpClient, CommandStateMachine.OnDataParseListener {

    private val logger = DefaultLogger()
    private var deviceHostSocket: Socket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mPackageReceiveListener: PackageReceivedListener? = null

    private val handlerThread = HandlerThread("HandlerThread")
    private var mSendHandler: Handler

    companion object {
        val tag = HttpClientViaSocket::class.simpleName!!
        private var instance: HttpClientViaSocket? = null
        fun getInstance(): HttpClientViaSocket {
            if (instance == null) {
                synchronized(HttpClientViaSocket::class.java) {
                    if (instance == null) {
                        instance = HttpClientViaSocket()
                    }
                }
            }
            return instance!!
        }
    }

    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null

    init {
        CommandStateMachine.getInstance().registerReceiveDataListener(this)
        handlerThread.start()
        mSendHandler = Handler(handlerThread.looper)
    }

    override fun connectSever(host: String, port: Int, callback: ConnectClientCallback?) {
        thread {
            try {
                deviceHostSocket = Socket(host, port)
                logger.info(tag, " 连接Server=${deviceHostSocket!!.inetAddress}  ${deviceHostSocket!!.port}  结果${deviceHostSocket!!.isConnected}")


                mInputStream = deviceHostSocket!!.getInputStream()
                mOutputStream = deviceHostSocket!!.getOutputStream()
                val receivedThread = object : ReceivedThread(mInputStream) {
                    override fun onDataReceived(bytes: ByteArray) {
                        CommandStateMachine.getInstance().parseData(bytes)
                    }
                }

                mainHandler.post {
                    if (deviceHostSocket!!.isConnected) {
                        callback?.onSuccess()
                    }
                }

                receivedThread.start()
                receivedThread.join()

            } catch (exception: Exception) {
                exception.printStackTrace()
                logger.info(tag, "Catch exception in startServer exception=$exception")
                mainHandler.post {
                    callback?.onFail(msg = exception.message)
                }

            }
        }
    }

    override fun disConnect() {
        deviceHostSocket?.close()

    }

    override fun sendPackage(packet: Packet, callback: (Boolean) -> Unit) {
        mSendHandler.post {
            try {
                if (mOutputStream == null) {
                    mainHandler.post { callback.invoke(false) }
                    return@post
                }
                val bytes = packet.encode()
                mOutputStream!!.write(bytes)
                logger.info(tag, ">>> cmd=${ByteUtils.byte2HexString(packet.cmd)} content=${ByteUtils.byteArray2HexString(packet.content)}")
                mainHandler.post { callback.invoke(true) }
            } catch (e: IOException) {
                e.printStackTrace()
                mainHandler.post { callback.invoke(false) }
            }
        }
    }


    override fun sendBytes(data: ByteArray, callback: (Boolean) -> Unit) {
        mSendHandler.post {
            try {
                if (mOutputStream == null) {
                    mainHandler.post { callback.invoke(false) }
                    return@post
                }
                mOutputStream!!.write(data)
                logger.info(tag, ">>> " + ByteUtils.byteArray2HexString(data))
                mainHandler.post { callback.invoke(true) }
            } catch (e: IOException) {
                logger.info(tag, "" + e)
                e.printStackTrace()
                mainHandler.post { callback.invoke(false) }
            }
        }
    }

    override fun setReceivePackageListener(listener: PackageReceivedListener?) {
        mPackageReceiveListener = listener
    }

    override fun setConnectChangeListener(connectChangeListener: ConnectChangeListener?) {
    }

    override fun ping(callback: (Boolean) -> Unit) {
        sendPackage(Packet.getPingPacket(), callback)
    }




    override fun onDataParseSuc(cmd: Byte, data: ByteArray) {
        logger.info(tag, "<<< cmd=${ByteUtils.byte2HexString(cmd)} ,  content= ${ByteUtils.byteArray2HexString(data)}")
        when (cmd) {
            CMD.PING.value -> {
                sendPackage(Packet.getPingPacket()) {
                }
            }
            else -> {
                mainHandler.post {
                    logger.info(tag, "post<<< cmd=${ByteUtils.byte2HexString(cmd)} , content= ${ByteUtils.byteArray2HexString(data)}")
                    mPackageReceiveListener?.onReceived(cmd, data)
                }
            }
        }
    }

    override fun onDataParseFail(state: CommandStateMachine.ReceiveDataState) {
        logger.info(tag, "<<<< onDataParseFail ${state.name}")
    }

}