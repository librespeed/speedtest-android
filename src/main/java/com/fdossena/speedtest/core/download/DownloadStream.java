package com.fdossena.speedtest.core.download;

import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.log.Logger;

public abstract class DownloadStream {
    private String server, path;
    private int ckSize;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Downloader downloader;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private long currentDownloaded=0, previouslyDownloaded=0;
    private boolean stopASAP=false;
    private Logger log;

    public DownloadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log){
        this.server=server;
        this.path=path;
        this.ckSize=ckSize;
        this.errorHandlingMode=errorHandlingMode;
        this.connectTimeout=connectTimeout;
        this.soTimeout=soTimeout;
        this.recvBuffer=recvBuffer;
        this.sendBuffer=sendBuffer;
        this.log=log;
        init();
    }

    private void init(){
        if(stopASAP) return;
        new Thread(){
            public void run(){
                if(c!=null){
                    try{c.close();}catch (Throwable t){}
                }
                if(downloader !=null) downloader.stopASAP();
                currentDownloaded=0;
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer);
                    if(stopASAP){
                        try{c.close();}catch (Throwable t){}
                        return;
                    }
                    downloader =new Downloader(c,path,ckSize) {
                        @Override
                        public void onProgress(long downloaded) {
                            currentDownloaded=downloaded;
                        }

                        @Override
                        public void onError(String err) {
                            log("A downloader died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                DownloadStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                previouslyDownloaded+=currentDownloaded;
                                Utils.sleep(100);
                                init();
                            }
                        }
                    };
                }catch (Throwable t){
                    log("A downloader failed hard");
                    try{c.close();}catch (Throwable t1){}
                    if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                        Utils.sleep(100);
                        init();
                    }else onError(t.toString());
                }
            }
        }.start();

    }

    public abstract void onError(String err);

    public void stopASAP(){
        stopASAP=true;
        if(downloader !=null) downloader.stopASAP();
    }

    public long getTotalDownloaded(){
        return previouslyDownloaded+currentDownloaded;
    }

    public void resetDownloadCounter(){
        previouslyDownloaded=0;
        currentDownloaded=0;
        if(downloader !=null) downloader.resetDownloadCounter();
    }

    public void join(){
        while(downloader==null) Utils.sleep(0,100);
        try{downloader.join();}catch (Throwable t){}
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
