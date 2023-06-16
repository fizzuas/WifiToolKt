package com.microport.wificonnector.core.api;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import com.microport.wificonnector.constant.Constant;
import com.microport.wificonnector.exception.LackOfPermissionException;
import com.microport.wificonnector.facade.OnWifiConnectStatusChangeListener;
import com.microport.wificonnector.facade.OnWifiScanListener;
import com.microport.wificonnector.facade.WifiInfo;
import com.microport.wificonnector.util.log.DefaultLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Android  sdk <=28 使用的连接API
 *
 * @author ouyx
 * @date 2023年04月04日
 */

public class WifiConnector {
    private static final String tag = WifiConnector.class.getSimpleName();
    private final DefaultLogger logger = new DefaultLogger(true, true);
    private Activity mActivity;


    private boolean isLinked = false;

    private OnWifiScanListener onWifiScanListener;
    private OnWifiConnectStatusChangeListener onWifiConnectStatusChangeListener;

    private BroadcastReceiver mWifiSearchBroadcastReceiver;
    private IntentFilter mWifiSearchIntentFilter;
    private BroadcastReceiver mWifiConnectBroadcastReceiver;
    private IntentFilter mWifiConnectIntentFilter;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    public WifiConnector(Activity context) {
        this.mActivity = context;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiApiTools.wifiManager = wifiManager;
        WifiApiTools.application = context.getApplication();
        init();
    }

    private List<ScanResult> mScanResultList = new ArrayList<>();

    private void init() {
        mWifiSearchBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {// 扫描结果改表
                    mScanResultList = WifiApiTools.getScanResults();
                    if (onWifiScanListener != null) {
                        onWifiScanListener.onScan(mScanResultList);
                    }
                }
            }
        };
        mWifiSearchIntentFilter = new IntentFilter();
        mWifiSearchIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mWifiConnectBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    int wifState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    if (wifState != WifiManager.WIFI_STATE_ENABLED) {
                        logger.error(tag, "Wifi模块启动失败");
                        if (onWifiConnectStatusChangeListener != null) {
                            onWifiConnectStatusChangeListener.onStatusChange(false, Constant.ERROR_DEVICE_NOT_HAVE_WIFI);
                        }
                    }
                } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                    if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                        logger.error(tag, "密码错误");
                        if (onWifiConnectStatusChangeListener != null) {
                            onWifiConnectStatusChangeListener.onStatusChange(false, Constant.ERROR_PASSWORD);
                        }
                    }
                } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo.DetailedState state = ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                    setWifiState(state);
                }
            }
        };
        mWifiConnectIntentFilter = new IntentFilter();
        mWifiConnectIntentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        mWifiConnectIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        //注册接收器
        mActivity.registerReceiver(mWifiSearchBroadcastReceiver, mWifiSearchIntentFilter);
        mActivity.registerReceiver(mWifiConnectBroadcastReceiver, mWifiConnectIntentFilter);
    }

    private boolean isConnected = false;

    private void setWifiState(NetworkInfo.DetailedState state) {
        if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
            logger.info(tag, "认证中");
        } else if (state == NetworkInfo.DetailedState.BLOCKED) {
            logger.info(tag, "阻塞");
        } else if (state == NetworkInfo.DetailedState.CONNECTED) {
            logger.info(tag, "连接成功");
            if (!isConnected) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (onWifiConnectStatusChangeListener != null) {
                                    onWifiConnectStatusChangeListener.onStatusChange(true, Constant.CONNECT_FINISH);
                                    if (ActivityCompat.checkSelfPermission(mActivity.getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        return;
                                    }
                                    onWifiConnectStatusChangeListener.onConnect(new WifiInfo(
                                            getConnectedWifi(),
                                            WifiApiTools.getIpAddress(),
                                            WifiApiTools.getMacAddress(),
                                            WifiApiTools.getGateway()
                                    ));
                                }
                            }
                        });
                    }
                }, 1000);
                isConnected = true;
            }
            isLinked = true;
        } else if (state == NetworkInfo.DetailedState.CONNECTING) {
            isLinked = false;
            logger.info(tag, "连接中: " + WifiApiTools.getSSID());
        } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
            isLinked = false;
            logger.info(tag, "已断开连接");
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(true, Constant.DISCONNECTED);
            }
        } else if (state == NetworkInfo.DetailedState.DISCONNECTING) {
            isLinked = false;
            logger.info(tag, "断开连接中");
        } else if (state == NetworkInfo.DetailedState.FAILED) {
            isLinked = false;
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(false, Constant.ERROR_CONNECT);
            }
            logger.info(tag, "连接失败");
        } else if (state == NetworkInfo.DetailedState.IDLE) {

        } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {

        } else if (state == NetworkInfo.DetailedState.SCANNING) {
            logger.info(tag, "搜索中");
        } else if (state == NetworkInfo.DetailedState.SUSPENDED) {

        }
    }


    /**
     * 获取已连接WifiS设备的 SSID
     *
     * @return SSID
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public String getConnectedWifi() {
        WifiManager wifiManager = ((WifiManager) mActivity.getApplicationContext().getSystemService(WIFI_SERVICE));
        android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String connectedWifiSSID = wifiInfo.getSSID();
        int networkId = wifiInfo.getNetworkId();
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new LackOfPermissionException("Invoke WifiManager#getConfiguredNetworks lack of permission Manifest.permission.ACCESS_FINE_LOCATION!");
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : configuredNetworks) {
            if (wifiConfiguration.networkId == networkId) {
                connectedWifiSSID = wifiConfiguration.SSID;
                break;
            }
        }
        return connectedWifiSSID;
    }

    public void close() {
        try {
            mActivity.unregisterReceiver(mWifiSearchBroadcastReceiver);
            mActivity.unregisterReceiver(mWifiConnectBroadcastReceiver);
        } catch (Exception e) {
            logger.error(tag, "Catch exception in method close" + e.getMessage());
        }
    }

    private WifiApiTools.WifiCipherType type = WifiApiTools.WifiCipherType.WIFICIPHER_NOPASS;

    private String ssid;
    private String password;

    public void connect(String ssid, String password, OnWifiConnectStatusChangeListener listener) {
        isConnected = false;
        if (mScanResultList.isEmpty()) {
            logger.error(tag, "此连接方式需要先进行查找");
            return;
        }
        logger.info(tag, "准备连接：" + ssid + " 密码：" + password);
        this.ssid = ssid;
        this.password = password;
        onWifiConnectStatusChangeListener = listener;
        type = WifiApiTools.getCipherType(ssid);

        if (ssid.equals(WifiApiTools.getSSID())) {
            logger.info(tag, "已连接");
            return;
        }
        if (mConnectAsyncTask != null) {
            mConnectAsyncTask.cancel(true);
            mConnectAsyncTask = null;
        }
        mConnectAsyncTask = new ConnectAsyncTask(ssid, password, type);
        mConnectAsyncTask.execute();
    }


    private ConnectAsyncTask mConnectAsyncTask = null;

    public void connect(String ssid, String password, WifiApiTools.WifiCipherType wifiCipherType, OnWifiConnectStatusChangeListener listener) {
        isConnected = false;
        this.ssid = ssid;
        this.password = password;
        logger.info(tag, "准备连接：" + ssid + " 密码：" + password);
        type = wifiCipherType;
        onWifiConnectStatusChangeListener = listener;

        if (ssid.equals(WifiApiTools.getSSID())) {
            logger.info(tag, "已连接");
            return;
        }
        if (mConnectAsyncTask != null) {
            mConnectAsyncTask.cancel(true);
            mConnectAsyncTask = null;
        }
        mConnectAsyncTask = new ConnectAsyncTask(ssid, password, type);
        mConnectAsyncTask.execute();
    }

    private WorkAsyncTask mWorkAsyncTask = null;

    public void scan(OnWifiScanListener listener) {
        onWifiScanListener = listener;
        if (mWorkAsyncTask != null) {
            mWorkAsyncTask.cancel(true);
            mWorkAsyncTask = null;
        }
        mWorkAsyncTask = new WorkAsyncTask();
        mWorkAsyncTask.execute();
    }

    public void stopScan() {
        if (mWorkAsyncTask != null) {
            mWorkAsyncTask.cancel(true);
            mWorkAsyncTask = null;
        }
    }

    /**
     * 获取wifi列表
     */
    private class WorkAsyncTask extends AsyncTask<Void, Void, List<ScanResult>> {
        private List<ScanResult> mScanResult = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            logger.info(tag, "搜索中...");
            if (onWifiScanListener != null) {
                onWifiScanListener.onScanStart();
            }
        }

        @Override
        protected List<ScanResult> doInBackground(Void... params) {
            if (WifiApiTools.startStan()) {
                mScanResult = WifiApiTools.getScanResults();
            }
            List<ScanResult> filterScanResultList = new ArrayList<>();
            if (mScanResult != null) {
                for (ScanResult wifi : mScanResult) {
                    filterScanResultList.add(wifi);
                    logger.info(tag, "查找到：" + wifi);
                }
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return filterScanResultList;
        }

        @Override
        protected void onPostExecute(final List<ScanResult> result) {
            super.onPostExecute(result);
            mScanResultList = result;
            if (onWifiScanListener != null) {
                onWifiScanListener.onScanStop(mScanResultList);
            }
        }
    }

    /**
     * 连接指定的wifi
     */
    class ConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private String ssid;
        private String password;
        private WifiApiTools.WifiCipherType type;
        WifiConfiguration tempConfig;

        public ConnectAsyncTask(String ssid, String password, WifiApiTools.WifiCipherType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(false, Constant.CONNECT_START);
            }
            logger.info(tag, "开始连接>>>");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // 打开wifi
            WifiApiTools.openWifi();
            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi，状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
            while (WifiApiTools.wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    logger.error(tag, "Catch exception " + ie.getMessage());
                }
            }

            tempConfig = WifiApiTools.getConfigViaSSID(mActivity.getApplicationContext(), ssid);
            //禁掉所有wifi
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                throw new LackOfPermissionException("wifiManager#getConfiguredNetworks need ACCESS_FINE_LOCATION PERMISSION ");
            }
            for (WifiConfiguration c : WifiApiTools.wifiManager.getConfiguredNetworks()) {
                WifiApiTools.wifiManager.disableNetwork(c.networkId);
            }
            if (tempConfig != null) {
                logger.info(tag, "[ssid="+ssid + "]是已存在配置，尝试连接");
                boolean enabled = WifiApiTools.wifiManager.enableNetwork(tempConfig.networkId, true);
                logger.info(tag, "设置网络配置：" + enabled);
                if (!isLinked && type != WifiApiTools.WifiCipherType.WIFICIPHER_NOPASS) {
                    try {
                        //超过5s提示失败
                        Thread.sleep(5000);
                        if (!isLinked) {
                            logger.info(tag, "[ssid="+ssid + "] 5S 后还没连接上");
                            WifiApiTools.wifiManager.disableNetwork(tempConfig.networkId);
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (onWifiConnectStatusChangeListener != null) {
                                        onWifiConnectStatusChangeListener.onStatusChange(false, Constant.ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return enabled;
            } else {
                logger.info(tag, "[SSID="+ssid + "]是新的配置，创建");
                if (type != WifiApiTools.WifiCipherType.WIFICIPHER_NOPASS) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WifiConfiguration wifiConfig = WifiApiTools.createWifiInfo(ssid, password, type);
                            logger.info(tag, "开始连接：" + wifiConfig.SSID);

                            int netID = WifiApiTools.wifiManager.addNetwork(wifiConfig);
                            boolean enabled = WifiApiTools.wifiManager.enableNetwork(netID, true);
                            logger.info(tag, "设置网络配置：" + enabled);
                        }
                    }).start();
                } else {
                    WifiConfiguration wifiConfig = WifiApiTools.createWifiInfo(ssid, password, type);
                    logger.info(tag, "开始连接：" + wifiConfig.SSID);
                    int netID = WifiApiTools.wifiManager.addNetwork(wifiConfig);
                    boolean enabled = WifiApiTools.wifiManager.enableNetwork(netID, true);
                    logger.info(tag, "设置网络配置：" + enabled);
                    return enabled;
                }
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mConnectAsyncTask = null;
        }
    }

    public void disconnectWifi() {
        WifiConfiguration tempConfig = WifiApiTools.getConfigViaSSID(mActivity, ssid);
        if (tempConfig != null) {
            WifiApiTools.wifiManager.removeNetwork(tempConfig.networkId);
            WifiApiTools.wifiManager.saveConfiguration();
        }
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            for (WifiConfiguration c : WifiApiTools.wifiManager.getConfiguredNetworks()) {
                WifiApiTools.wifiManager.disableNetwork(c.networkId);
            }
        }

        WifiApiTools.wifiManager.disconnect();
        WifiApiTools.wifiManager.setWifiEnabled(false);
    }


}
