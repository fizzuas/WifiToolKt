/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.wifi_client.page.transport.adapter

import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.wifi_client.R


/**
 *
 * @author ouyx
 * @date 2023年04月19日 15时14分
 */
class ImageLocalBinder : QuickItemBinder<ImageLocal>() {
    override fun convert(holder: BaseViewHolder, data: ImageLocal) {
        if (data.sendSuccess) {
            holder.getView<TextView>(R.id.tv_tip).text = ""
            val imageView = holder.getView<ImageView>(R.id.img_local)
            Glide.with(context).load(data.content).into(imageView)
        } else {
            holder.getView<TextView>(R.id.tv_tip).text = "发送失败"
            val imageView = holder.getView<ImageView>(R.id.img_local)
            Glide.with(context).load(data.content).into(imageView)
        }
    }

    override fun getLayoutId(): Int = R.layout.item_iamge_local
}