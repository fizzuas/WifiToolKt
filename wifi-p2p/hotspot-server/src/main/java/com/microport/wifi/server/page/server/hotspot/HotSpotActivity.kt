package com.microport.wifi.server.page.server.hotspot

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.microport.wifi.server.databinding.ActivityHotSpotBinding
import com.microport.wifi.server.page.server.service.OnProgressChangListener
import com.microport.wifi.server.page.server.service.WifiServerService
import github.leavesc.wifip2p.model.FileTransfer
import timber.log.Timber
import java.io.File


class HotSpotActivity : AppCompatActivity() {
    private val tag=HotSpotActivity::class.java.name

    val mProgressListener = object : OnProgressChangListener {
        override fun onProgressChanged(fileTransfer: FileTransfer?, progress: Int) {
        }

        override fun onTransferFinished(file: File?) {
            Timber.i("onTransferFinished：${file?.name}")
            runOnUiThread {
                if (file != null && file.exists()) {
                    Glide.with(this@HotSpotActivity).load(file.path).into(mViewBinding.img)
                }
            }
        }
    }

    private val mMainHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }
    lateinit var mViewBinding: ActivityHotSpotBinding

    private var wifiServerService: WifiServerService? = null


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.i("onServiceConnected \t${name}")
            val binder = service as WifiServerService.WifiServerBinder
            wifiServerService = binder.service
            wifiServerService?.setProgressChangListener(mProgressListener)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.i("onServiceDisconnected \t${name}")
            wifiServerService?.setProgressChangListener(null)
            wifiServerService = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityHotSpotBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        initView()
        bindService()
    }

    private fun initView() {
        mViewBinding.butCreateHotspot.setOnClickListener {openHotSpot()}
        mViewBinding.butStartServer.setOnClickListener {
            wifiServerService?.let {
                val intent = Intent(this, WifiServerService::class.java)
                startService(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openHotSpot() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                val wifiConfiguration = reservation.wifiConfiguration
                if (wifiConfiguration != null) {
                    Log.i(tag,"start wifi hotspot  cfg[ssid=${wifiConfiguration.SSID},pwd=${wifiConfiguration.preSharedKey}]")
                }
                // success
                Timber.i("onSuccess")
            }

            override fun onStopped() {
                super.onStopped()
                Timber.i("onStopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Timber.i("onFailed")
            }
        }, mMainHandler)

    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        stopService(Intent(this, WifiServerService::class.java))
    }

//    /** * android8.0以上开启手机热点 */
//    private fun startTethering() {
//        val connectivityManager =
//            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        try {
//
//
//            val outputDir = codeCacheDir
//            val classOnStartTetheringCallback =
//                Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback");
//            val startTethering = connectivityManager::class.java.getDeclaredMethod(
//                "startTethering",
//                Int::class.java,
//                Boolean::class.java,
//                classOnStartTetheringCallback
//            );
//            val proxy = ProxyBuilder.forClass(classOnStartTetheringCallback).dexCache(
//                outputDir
//            ).handler(new InvocationHandler () {
//                @Override public Object invoke(
//                    Object o, Method method, Object[] objects
//                ) throws Throwable {
//                    return null;
//                }
//            }).build();
//            startTethering.invoke(connectivityManager, 0, false, proxy);
//        } catch (Exception e) {
//            Log.e(TAG, "打开热点失败");
//            e.printStackTrace();
//        }
//    }



    private fun bindService() {
        val intent = Intent(this, WifiServerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }
}