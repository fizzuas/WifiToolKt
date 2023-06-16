/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.page.transport.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.hospot_server.R


/**
 * 本地 客户端 发送的文字 对应的 RecyclerView Item
 *
 * @author ouyx
 * @date 2023年04月17日 14时53分
 */
data class TextLocal(val content: String?, val sendSuccess: Boolean)

class TextLocalBinder : QuickItemBinder<TextLocal>() {
    override fun convert(holder: BaseViewHolder, data: TextLocal) {
        if (data.sendSuccess) {
            holder.getView<TextView>(R.id.tv_local).setTextColor(Color.BLACK)
            holder.getView<TextView>(R.id.tv_local).text = data.content
        } else {
            holder.getView<TextView>(R.id.tv_local).setTextColor(Color.GRAY)
            holder.getView<TextView>(R.id.tv_local).text = data.content + "发送失败"
        }
    }

    override fun getLayoutId(): Int = R.layout.item_text_local
}