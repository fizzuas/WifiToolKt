/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.hospot_server.page.transport.adapter


/**
 *
 * @author ouyx
 * @date 2023年04月19日 16时03分
 */
data class ImageRemote(val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageRemote

        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        return content.contentHashCode()
    }
}