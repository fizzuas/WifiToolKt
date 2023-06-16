package com.microport.hospot_server.page.home.page.transport


import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseBinderAdapter
import com.microport.hospot_server.databinding.ActivityTransportBinding
import com.microport.hospot_server.base.BaseVmActivity
import com.microport.hospot_server.page.home.page.transport.adapter.*
import com.microport.httpserver.contract.CMD
import com.microport.httpserver.contract.Packet
import com.microport.httpserver.contract.content.ImageContent
import com.microport.httpserver.facade.interfaces.IHttpServer
import com.microport.httpserver.facade.interfaces.PackageReceivedListener
import com.microport.httpserver.launch.HttpServer
import com.microport.httpserver.util.ByteUtils
import com.microport.httpserver.util.log.DefaultLogger

class TransportActivity : BaseVmActivity<ActivityTransportBinding, TransportModel>(ActivityTransportBinding::inflate), PackageReceivedListener {
    private val tag = TransportActivity::class.java.simpleName
    private val logger = DefaultLogger()
    private val mHttpServer: IHttpServer = HttpServer.getInstance()

    private val mContractUIAdapter = BaseBinderAdapter()
    private val mData = mutableListOf<Any>()

    override fun viewModelClass(): Class<TransportModel> = TransportModel::class.java


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHttpServer.setReceivePackageListener(this)
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

            logger.info(tag, "hexString=$content,length=${content.length}")
            val packet = Packet(cmd = CMD.HEX_STRING.value, content = ByteUtils.hexString2byteArray(content))
            mHttpServer.sendPackage(packet) {
                mContractUIAdapter.data.add(TextLocal(content, it))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
        }

        mBinding.butSendMsg.setOnClickListener {
            val content = mBinding.etInput.text.toString().trim()
            val packet = Packet(cmd = CMD.STRING_UTF8.value, content = content.toByteArray())
            mHttpServer.sendPackage(packet) {
                mContractUIAdapter.data.add(TextLocal(content, it))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
        }

        mContractUIAdapter.apply {
            addItemBinder(TextRemote::class.java, TextRemoteBinder())
            addItemBinder(TextLocal::class.java, TextLocalBinder())
            addItemBinder(ImageRemote::class.java, ImageRemoteBinder())
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
                mContractUIAdapter.data.add(TextRemote(content = ByteUtils.byteArray2HexString(data)))
                mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
            }
            CMD.STRING_UTF8.value -> {
                val content: String? = data?.let { String(it) }
                content?.let {
                    mContractUIAdapter.data.add(TextRemote(content = content))
                    mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
                }
            }
            CMD.IMG.value, CMD.IMG_PROTO.value -> {
                val imageContent: ImageContent? = data?.let { ImageContent.decode(data) }
                imageContent?.let {
                    mContractUIAdapter.data.add(ImageRemote(content = imageContent.imgBytes))
                    mContractUIAdapter.notifyItemInserted(mContractUIAdapter.data.lastIndex)
                }
            }
        }
    }


}