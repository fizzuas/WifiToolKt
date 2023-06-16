/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.page.transport.adapter

import android.widget.TextView
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.hospot_server.R


/**
 *  Remote Client 发送文字 对应 的RecyclerView UI ITEM
 *
 * @author ouyx
 * @date 2023年04月17日 14时45分
 */
data class TextRemote(val content: String)

class TextRemoteBinder : QuickItemBinder<TextRemote>() {
    override fun convert(holder: BaseViewHolder, data: TextRemote) {
        holder.getView<TextView>(R.id.tv_remote).text = data.content
    }

    override fun getLayoutId(): Int = R.layout.item_text_remote
}