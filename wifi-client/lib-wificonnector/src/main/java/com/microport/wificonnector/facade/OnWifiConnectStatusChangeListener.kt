/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.wificonnector.facade

/**
 *
 *
 * @author ouyx
 * @date 2023年04月04日
 */
abstract class OnWifiConnectStatusChangeListener {
    abstract fun onStatusChange(isSuccess: Boolean, statusCode: Int)
    abstract fun onConnect(wifiInfo: WifiInfo)
}