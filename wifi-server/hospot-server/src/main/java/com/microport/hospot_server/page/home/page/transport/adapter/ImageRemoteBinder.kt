/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.page.home.page.transport.adapter

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.hospot_server.R


/**
 * TODO:
 *
 * @author admin
 * @date 2023年04月19日 16时03分
 */
class ImageRemoteBinder : QuickItemBinder<ImageRemote>() {
    override fun convert(holder: BaseViewHolder, data: ImageRemote) {
        val imageView = holder.getView<ImageView>(R.id.img_remote)
        Glide.with(context).load(data.content).into(imageView)
    }

    override fun getLayoutId(): Int = R.layout.item_image_remote
}