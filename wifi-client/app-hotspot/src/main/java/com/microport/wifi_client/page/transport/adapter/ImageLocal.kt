/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.wifi_client.page.transport.adapter

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.wifi_client.R
import org.w3c.dom.Text


/**
 *
 *
 * @author ouyx
 * @date 2023年04月19日 14时22分
 */
data class ImageLocal(val content: ByteArray, val sendSuccess: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageLocal

        if (!content.contentEquals(other.content)) return false
        if (sendSuccess != other.sendSuccess) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + sendSuccess.hashCode()
        return result
    }
}


