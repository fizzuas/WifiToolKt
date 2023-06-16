/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.page.home.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.microport.hospot_server.R


/**
 * ouyx
 *
 * @author admin
 * @date 2023年04月13日 15时03分
 */
class ConnectInfoListAdapter(layoutId: Int = R.layout.item_simple, list: MutableList<String>) :
    BaseQuickAdapter<String, BaseViewHolder>(layoutId, list) {
    override fun convert(holder: BaseViewHolder, item: String) {
        holder.setText(R.id.tv_item_simple, item)
    }
}