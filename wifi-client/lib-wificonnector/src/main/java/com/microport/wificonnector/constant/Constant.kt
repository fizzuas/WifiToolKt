package com.microport.wificonnector.constant


/**
 *
 * @author ouyx
 * @date 2023年04月07日 17时00分
 */
object Constant {
    const val ERROR_DEVICE_NOT_HAVE_WIFI = -1 //设备无Wifi模块

    const val ERROR_CONNECT = -2 //连接失败

    const val ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG = -3 //连接失败：系统已存在相同Wifi配置（需手动删除已存储连接）

    const val ERROR_PASSWORD = -11 //密码错误


    const val CONNECT_START = 1 //开始连接

    const val CONNECT_FINISH = 2 //已连接

    const val DISCONNECTED = 3 //已断开连接

}