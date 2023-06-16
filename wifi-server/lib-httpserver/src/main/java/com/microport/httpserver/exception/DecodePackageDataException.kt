/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.exception


/**
 * 接收数据解码成 [com.microport.httpserver.contract.Packet] 出现的异常
 *
 * @author ouyx
 * @date 2023年03月24日 14时09分
 */

 class DecodePackageDataException(message: String?) : RuntimeException(message)