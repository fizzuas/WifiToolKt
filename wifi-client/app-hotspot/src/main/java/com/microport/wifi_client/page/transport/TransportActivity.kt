package com.microport.wifi_client.page.transport


import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

import com.chad.library.adapter.base.BaseBinderAdapter
import com.client.util.ByteUtils
import com.microport.lib_httpclient.contract.CMD
import com.microport.lib_httpclient.contract.Packet
import com.microport.lib_httpclient.contract.content.ImageContent
import com.microport.lib_httpclient.facade.IHttpClient
import com.microport.lib_httpclient.facade.PackageReceivedListener
import com.microport.lib_httpclient.launch.HttpClient
import com.microport.wifi_client.base.BaseVmActivity
import com.microport.wifi_client.databinding.ActivityTransportBinding
import com.microport.wifi_client.page.transport.adapter.*
import com.microport.wifi_client.util.BitmapUtil
import com.microport.wifi_client.util.log.DefaultLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class TransportActivity : BaseVmActivity<ActivityTransportBinding, TransportModel>(ActivityTransportBinding::inflate), PackageReceivedListener {
    private val tag = TransportActivity::class.java.simpleName
    private val logger = DefaultLogger()
    private val mHttpClient: IHttpClient = HttpClient.getInstance()

    private val mContractUIAdapter = BaseBinderAdapter()
    private val mData = mutableListOf<Any>()


    override fun viewModelClass()=TransportModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHttpClient.setReceivePackageListener(this)
        mBinding.butHex.setOnClickListener {
            val content = mBinding.etInput.text.toString().trim()
            if (!ByteUtils.isHexStr(content)) {
                Toast.makeText(this, "输入必须是HEX", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.length % 2 != 0) {
                Toast.makeText(this, "输入长度必须是偶数", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val packet = Packet(cmd = CMD.HEX_STRING.value, content = ByteUtils.hexString2byteArray(content))
            mHttpClient.sendPackage(packet) {
                mContractUIAdapter.data.add(TextLocal(content, it))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
        }

        mBinding.butSendMsg.setOnClickListener {
            val content = mBinding.etInput.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "输入不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val packet = Packet(cmd = CMD.STRING_UTF8.value, content = content.toByteArray())
            mHttpClient.sendPackage(packet) {
                mContractUIAdapter.data.add(TextLocal(content, it))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
        }

        mBinding.butSendImage.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmapBytes = BitmapUtil.readBitmap(applicationContext, "data/1.png")
                val imageContent = ImageContent(0, 0, bitmapBytes)
                mHttpClient.sendPackage(Packet(cmd = CMD.IMG.value, imageContent.encode())) {
                    mContractUIAdapter.data.add(ImageLocal(bitmapBytes, it))
                    mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
                }
            }
        }
        mBinding.butSendImages.setOnClickListener {
            excuteTask {
                mBinding.butSendImages.text = "hah"
            }
        }

        mContractUIAdapter.apply {
            addItemBinder(TextRemote::class.java, TextRemoteBinder())
            addItemBinder(TextLocal::class.java, TextLocalBinder())
            addItemBinder(ImageLocal::class.java, ImageLocalBinder())
            setList(mData)
        }

        mBinding.recyclerViewContent.apply {
            adapter = mContractUIAdapter
            layoutManager = LinearLayoutManager(this@TransportActivity)
        }

    }

    override fun onReceived(cmd: Byte, data: ByteArray?) {
        when (cmd) {
            CMD.HEX_STRING.value, CMD.PING.value -> {
                val content = ByteUtils.byteArray2HexString(data)
                logger.info(tag, "content =${content}")
                mContractUIAdapter.data.add(TextRemote(content = ByteUtils.byteArray2HexString(data)))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
            CMD.STRING_UTF8.value -> {
                val content = data?.let { String(it) }
                mContractUIAdapter.data.add(TextRemote(content = content))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
            CMD.IMG.value, CMD.IMG_PROTO.value -> {

            }
        }
    }

    fun excuteTask(callback: (Boolean) -> Unit) {
//        thread {
//            Thread.sleep(5000)
//            mainHandler.post() {
//                callback.invoke(true)
//            }
//        }
    }
}