package com.fdossena.speedtest.core.upload;

import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.log.Logger;

public abstract class UploadStream {
    private String server, path;
    private int ckSize;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private Connection c=null;
    private Uploader uploader;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private long currentUploaded=0, previouslyUploaded=0;
    private boolean stopASAP=false;
    private Logger log;

    public UploadStream(String server, String path, int ckSize, String errorHandlingMode, int connectTimeout, int soTimeout, int recvBuffer, int sendBuffer, Logger log){
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
                if(uploader !=null) uploader.stopASAP();
                currentUploaded=0;
                try {
                    c = new Connection(server, connectTimeout, soTimeout, recvBuffer, sendBuffer);
                    if(stopASAP){
                        try{c.close();}catch (Throwable t){}
                        return;
                    }
                    uploader =new Uploader(c,path,ckSize) {
                        @Override
                        public void onProgress(long uploaded) {
                            currentUploaded=uploaded;
                        }

                        @Override
                        public void onError(String err) {
                            log("An uploader died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                UploadStream.this.onError(err);
                                return;
                            }
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_ATTEMPT_RESTART)||errorHandlingMode.equals(SpeedtestConfig.ONERROR_MUST_RESTART)){
                                previouslyUploaded+=currentUploaded;
                                Utils.sleep(100);
                                init();
                            }
                        }
                    };
                }catch (Throwable t){
                    log("An uploader failed hard");
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
        if(uploader !=null) uploader.stopASAP();
    }

    public long getTotalUploaded(){
        return previouslyUploaded+currentUploaded;
    }

    public void resetUploadCounter(){
        previouslyUploaded=0;
        currentUploaded=0;
        if(uploader !=null) uploader.resetUploadCounter();
    }

    public void join(){
        while(uploader==null) Utils.sleep(0,100);
        try{uploader.join();}catch (Throwable t){}
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
