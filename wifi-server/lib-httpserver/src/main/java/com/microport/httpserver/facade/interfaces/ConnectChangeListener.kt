package com.microport.httpserver.facade.interfaces

/**
 * @author ouyx
 * @date 2022/11/24 17:56
 *
 */
interface ConnectChangeListener {

    fun onConnected(id: String?, clientIP: String?)
    fun onDisConnect(id: String?, clientIP: String?)

}