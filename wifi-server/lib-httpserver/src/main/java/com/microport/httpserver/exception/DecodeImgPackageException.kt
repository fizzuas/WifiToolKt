/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.microport.httpserver.exception


/**
 * [com.microport.httpserver.contract.Packet]中Content解码成 [com.microport.httpserver.contract.content.ImageContent] 出现的异常
 *
 * @author admin
 * @date 2023年03月24日 17时06分
 */
class DecodeImgPackageException(message: String?) : RuntimeException(message)