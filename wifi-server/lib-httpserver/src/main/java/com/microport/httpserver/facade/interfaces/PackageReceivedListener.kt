package com.microport.httpserver.facade.interfaces

/**
 * @author ouyx
 * @date 2022/11/24 17:41
 *
 */
interface PackageReceivedListener  {
    fun onReceived(cmd: Byte, data: ByteArray?)
}