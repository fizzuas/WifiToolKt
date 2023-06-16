package com.microport.httpserver.util.log

interface ILogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warning(tag: String, message: String)
    fun error(tag: String, message: String)
    fun error(tag: String, message: String, e: Throwable?)
    fun isDebug():Boolean
}