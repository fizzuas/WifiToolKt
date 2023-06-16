/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.facade.interfaces


/**
 * [com.microport.httpserver.facade.interfaces.IHttpServer.startServer] 回调
 *
 * @author admin
 * @date 2023年04月12日 16时03分
 */
interface StartServerCallback {
    fun onSuccess()
    fun onFail(msg: String?)
}