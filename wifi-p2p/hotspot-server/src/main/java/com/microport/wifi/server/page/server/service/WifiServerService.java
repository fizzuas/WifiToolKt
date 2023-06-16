package com.microport.wifi.server.page.server.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.microport.wifi.server.page.util.Md5Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import github.leavesc.wifip2p.model.FileTransfer;
import timber.log.Timber;

public class WifiServerService extends IntentService {

    private static final String TAG = "WifiServerService";
    private static final int PORT = 1995;

    private ServerSocket serverSocket;

    private InputStream inputStream;

    private ObjectInputStream objectInputStream;

    private FileOutputStream fileOutputStream;

    private OnProgressChangListener mProgressListener;

    public WifiServerService() {
        super("WifiServerService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new WifiServerBinder();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        clean();
        File file = null;
        try {
            Timber.tag(TAG).e("开启服务->onHandleIntent");
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            Socket client = serverSocket.accept();
            Timber.tag(TAG).e("客户端IP地址 : %s", client.getInetAddress().getHostAddress());
            inputStream = client.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            Timber.tag(TAG).e("待接收的fileTransfer: %s", fileTransfer.toString());
            String name = fileTransfer.getFileName();
            //将文件存储至指定位置
            file = new File(getCacheDir(), name);
            fileOutputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            long total = 0;
            int progress;
            while ((len = inputStream.read(buf)) != -1) {
                fileOutputStream.write(buf, 0, len);
                total += len;
                progress = (int) ((total * 100) / fileTransfer.getFileLength());
                Timber.tag(TAG).e("文件接收进度: %s", progress);
                if (mProgressListener != null) {
                    Timber.tag(TAG).i("onProgressChanged%s", progress);
                    mProgressListener.onProgressChanged(fileTransfer,progress);
                }
            }
            serverSocket.close();
            inputStream.close();
            objectInputStream.close();
            fileOutputStream.close();
            serverSocket = null;
            inputStream = null;
            objectInputStream = null;
            fileOutputStream = null;
            Timber.tag(TAG).e("文件接收成功，文件的MD5码是：%s", Md5Util.getMd5(file));
        } catch (Exception e) {
            Timber.tag(TAG).e("文件接收 Exception: %s", e.getMessage());
        } finally {
            clean();
            if (mProgressListener != null) {
                mProgressListener.onTransferFinished(file);
            }
            //再次启动服务，等待客户端下次连接
//            startService(new Intent(this, WifiServerService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clean();
    }

    public void setProgressChangListener(OnProgressChangListener listener) {
        mProgressListener = listener;
    }

    private void clean() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (objectInputStream != null) {
            try {
                objectInputStream.close();
                objectInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
                fileOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class WifiServerBinder extends Binder {
        public WifiServerService getService() {
            return WifiServerService.this;
        }
    }

}
