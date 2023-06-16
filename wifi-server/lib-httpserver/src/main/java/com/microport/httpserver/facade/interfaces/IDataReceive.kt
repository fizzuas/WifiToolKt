package com.microport.httpserver.facade.interfaces

/**
 * @author ouyx
 * @date 2022/11/25 17:06
 *
 */
interface IDataReceive {
    fun onDataReceived(bytes: ByteArray)
}