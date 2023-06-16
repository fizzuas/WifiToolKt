package com.microport.wifilib.p2p.api

enum class Error {
    P2P_UNSUPPORTED, BUSY, NO_PERMISSION, WIFI_NO_ENABLE, TIME_OUT, UNKNOWN;

    fun getMsg(): String {
        return when (this) {
            P2P_UNSUPPORTED -> "不支持P2P"
            BUSY -> "设备忙碌"
            NO_PERMISSION -> "权限不够"
            WIFI_NO_ENABLE -> "WIFI未开启"
            TIME_OUT -> "超时"
            UNKNOWN -> "未知错误"
        }
    }
}


fun transferActionCode(actionFailCode: Int): Error {
    return when (actionFailCode) {
        1 -> Error.P2P_UNSUPPORTED
        2 -> Error.BUSY
        else -> Error.UNKNOWN
    }
}