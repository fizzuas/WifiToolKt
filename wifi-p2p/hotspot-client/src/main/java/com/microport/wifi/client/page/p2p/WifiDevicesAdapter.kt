package com.microport.wifi.client.page.p2p

import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.wifi.client.R
import com.microport.wifilib.p2p.param.DeviceInfo
import com.microport.wifilib.p2p.param.getDeviceStatus

class WifiDevicesAdapter(layoutResId: Int = R.layout.item_wifi, data: MutableList<DeviceInfo>?) :
    BaseQuickAdapter<DeviceInfo, BaseViewHolder>(layoutResId, data) {
    override fun convert(holder: BaseViewHolder, item: DeviceInfo) {
        holder.getView<TextView>(R.id.tv_name).text = item.device.deviceName
        holder.getView<TextView>(R.id.tv_address).text = item.device.deviceAddress
        holder.getView<TextView>(R.id.tv_state).text = getDeviceStatus(item.device.status)
    }
}