package com.microport.httpserver.contract

/**
 * @author ouyx
 * @date 2022/11/28 16:45
 *
 */

enum class CMD(val value: Byte) {
    PING(0x00), IMG(0x01), IMG_PROTO(0x02),HEX_STRING(0X04),STRING_UTF8(0x05)


}

