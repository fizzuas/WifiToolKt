package com.microport.wifi.server.page.server.service;

import java.io.File;

import github.leavesc.wifip2p.model.FileTransfer;

public interface OnProgressChangListener {
    //当传输进度发生变化时
    void onProgressChanged(FileTransfer fileTransfer, int progress);

    //当传输结束时
    void onTransferFinished(File file);
}
