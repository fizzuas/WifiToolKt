
# 启动基于Socket实现的 Server



# 启动 基于Netty实现的Server
val mAdbUsbServer: IAdbUsbServer = NettyAdbUsbServer.getInstance()

# 使用方法
interface IAdbUsbServer {
fun startServer()
fun sendPackage(packageData:PackageData)
fun sendBytes(data: ByteArray)
fun setReceivePackageListener(listener: PackageReceivedListener?)
fun setConnectStateChangeListener(connectStateChangeListener: ConnectStateChangeListener?)
fun ping(connectCallback: (Boolean) -> Unit)
}

代码参考 app模块下com.microport.adbusbserver.MainActivity 