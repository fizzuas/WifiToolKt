package com.microport.httpserver.core.socket

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.microport.httpserver.contract.CMD
import com.microport.httpserver.contract.Packet
import com.microport.httpserver.contract.content.PING_CONTENT
import com.microport.httpserver.core.socket.transport.CommandStateMachine
import com.microport.httpserver.core.socket.transport.UsbReceivedThread
import com.microport.httpserver.core.socket.transport.UsbReceivedThread.Companion.USB_RECEIVED_THREAD_NAME_PREFIX
import com.microport.httpserver.facade.interfaces.*
import com.microport.httpserver.util.ByteUtils
import com.microport.httpserver.util.log.DefaultLogger
import com.microport.httpserver.util.log.ILogger
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * 基于Netty实现的HttpServer
 *
 *  @author ouyx
 * @date 2022/11/24 15:19
 *
 */
internal class HttpServerViaSocket private constructor() : IHttpServer, IDataReceive, CommandStateMachine.OnDataParseListener {
    private val tag = HttpServerViaSocket::class.java.simpleName
    private val log: ILogger = DefaultLogger()

    private var mSocket: Socket? = null
    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null
    private var mReceivedThread: UsbReceivedThread? = null

    private var mSendHandlerThread: HandlerThread = HandlerThread("ADB_SERVER_SENDER")
    private var mSendHandler: Handler

    private val mMainHandler = Handler(Looper.getMainLooper())

    private var mReceivedPackageListener: PackageReceivedListener? = null
    private var mConnectStateChangeListener: ConnectChangeListener? = null


    private var mPingCountDownLatch: CountDownLatch? = null

    @Volatile
    private var isRunning = false

    private val receivedThreadNumber: AtomicInteger = AtomicInteger(1)

    companion object {
        const val MSG_SEND_BYTES = 0
    }

    init {
        mSendHandlerThread.start()
        mSendHandler = Handler(mSendHandlerThread.looper) {
            when (it.what) {
                MSG_SEND_BYTES -> {
                    sendBytesReal(it.obj as ByteArray)
                }
            }
            true
        }
        CommandStateMachine.getInstance().registerReceiveDataListener(this)
    }

    override fun startServer(port: Int, callback: StartServerCallback?) {
        if (isRunning) {
            log.error(tag, "Server is running!")
            return
        }
        Thread({
            try {
                val serverSocket = ServerSocket(port)
                log.info(tag, "ServerSocket is waiting for connect on port ${port}...")
                isRunning = true
                while (true) {
                    mSocket = serverSocket.accept()
                    log.info(tag, "ServerSocket accepted usb connect address [remoteSocketAddress=${mSocket?.remoteSocketAddress}]\t ")
                    mSocket?.let {
                        mReceivedThread?.release()

                        mInputStream = it.getInputStream()
                        mOutputStream = it.getOutputStream()
                        mReceivedThread = UsbReceivedThread(
                            it.getInputStream(),
                            dataReceive = this@HttpServerViaSocket,
                            name = "$USB_RECEIVED_THREAD_NAME_PREFIX-${receivedThreadNumber.getAndIncrement()}"
                        )
                        mReceivedThread?.start()
                        val ip = (mSocket!!.remoteSocketAddress as InetSocketAddress).address.hostAddress
                        mConnectStateChangeListener?.onConnected(id = null, clientIP = ip)
                    }
                }
            } catch (e: IOException) {
                log.error(tag, "ServerSocket appeared exception!,int thread[${Thread.currentThread().name}],because [${e.message}] ")
            }
        }, "THREAD_ADB_SERVER").start()
    }


    override fun sendPackage(aPacket: Packet, callback: (Boolean) -> Unit) {
        val bytes = aPacket.encode()
        sendBytes(bytes,callback)
    }


    override fun sendBytes(data: ByteArray,callback: (Boolean) -> Unit) {
        val msg = Message()
        msg.what = MSG_SEND_BYTES
        msg.obj = data
        mSendHandler.sendMessage(msg)
    }


    override fun setReceivePackageListener(listener: PackageReceivedListener?) {
        mReceivedPackageListener = listener
    }


    override fun setConnectChangeListener(connectChangeListener: ConnectChangeListener?) {
        mConnectStateChangeListener = connectChangeListener
    }


    override fun ping(callback: (Boolean) -> Unit) {
        thread {
            mPingCountDownLatch = CountDownLatch(1)
            try {
                val data = Packet(CMD.PING.value, PING_CONTENT).encode()
                mOutputStream!!.write(data)
                mOutputStream!!.flush()
            } catch (e: Exception) {
                log.error(tag, "send bytes fail! because[${e.message}]")
                mMainHandler.post { callback(false) }
                return@thread
            }
            log.info(
                tag, ">>> ping cmd=${CMD.PING.value}   content =${
                    ByteUtils.byteArray2HexString(byteArrayOf(0x00))
                }"
            )
            try {
                val rst = mPingCountDownLatch!!.await(3, TimeUnit.SECONDS)
                if (rst) {
                    mMainHandler.post { callback(true) }
                    return@thread
                }
            } catch (e: InterruptedException) {
                log.error(tag, "method isConnect catch exception,because[${e.message}]")
            }
            mMainHandler.post { callback(false) }

        }
    }

    override fun onDataReceived(bytes: ByteArray) {
        CommandStateMachine.getInstance().parseData(bytes)
    }

    override fun onDataParseSuc(cmd: Byte, data: List<Byte>) {
        if (data.size < 50) {
            log.info(
                tag, "<<< cmd=${ByteUtils.byte2HexString(cmd)} , data = ${
                    ByteUtils.byteArray2HexString(data.toByteArray())
                }"
            )
        } else {
            log.info(
                tag, "<<< cmd=${ByteUtils.byte2HexString(cmd)} , data = ${
                    ByteUtils.byteArray2HexString(data.subList(0, 50).toByteArray())
                }"
            )
        }
        if (cmd == CMD.PING.value) {
            try {
                mPingCountDownLatch?.countDown()
            } catch (e: InterruptedException) {
                //ignore
            }
        } else {
            mReceivedPackageListener?.onReceived(cmd, data.toByteArray())
        }
    }

    override fun onDataParseFail(state: CommandStateMachine.ReceiveDataState) {
        log.error(tag, "<<< 解析失败 ${state.name}")
    }

    private fun sendBytesReal(data: ByteArray) {
        if (mOutputStream == null) {
            log.error(tag, "abort send! because[mOutputStream is null]")
            return
        }
        try {
            if (data.size < 50) {
                log.info(tag, ">>> ${ByteUtils.byteArray2HexString(data)}")
            } else {
                log.info(tag, ">>>(0,50)${ByteUtils.byteArray2HexString(data.sliceArray(IntRange(0, 50)))}")
            }
            mOutputStream!!.write(data)
            mOutputStream!!.flush()
        } catch (e: IOException) {
            log.error(tag, "send bytes fail! $e")
        }
    }

}