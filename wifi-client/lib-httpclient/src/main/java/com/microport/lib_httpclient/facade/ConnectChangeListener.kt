package com.microport.lib_httpclient.facade

/**
 * @author ouyx
 * @date 2022/11/24 17:56
 *
 */
interface ConnectChangeListener {
    fun onConnected(connection: Connection)
    fun onDisConnect(connection: Connection)

}