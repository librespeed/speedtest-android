package com.fdossena.speedtest.core.upload;

import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.log.Logger;

public abstract class UploadStream {
    private String server, path;
    private int ckSize;
    private int connectTimeout, soTimeout, recvBuffer, sendBuffer;
    private volatile Connection c=null;
    private volatile Uploader uploader;
    private String errorHandlingMode= SpeedtestConfig.ONERROR_ATTEMPT_RESTART;
    private volatile long currentUploaded=0, previouslyUploaded=0;
    //ended means no uploader will ever (re)appear: hard failure or stopped
    //before one was created; join() must not keep waiting past it
    private volatile boolean stopASAP=false, ended=false;
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
                        ended=true;
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
                            if(stopASAP) return;
                            log("An uploader died");
                            if(errorHandlingMode.equals(SpeedtestConfig.ONERROR_FAIL)){
                                ended=true;
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
                    }else{
                        ended=true;
                        onError(t.toString());
                    }
                }
            }
        }.start();
    }

    public abstract void onError(String err);

    public void stopASAP(){
        stopASAP=true;
        if(uploader !=null) uploader.stopASAP();
        //closing the connection unblocks a thread parked in a read or write,
        //making the stop prompt instead of waiting out the socket timeout
        Connection conn=c;
        if(conn!=null){
            try{conn.close();}catch (Throwable t){}
        }
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
        while(uploader==null&&!ended&&!stopASAP) Utils.sleep(1);
        Uploader u=uploader;
        if(u!=null){
            try{u.join();}catch (Throwable t){}
        }
    }

    private void log(String s){
        if(log!=null) log.l(s);
    }

}
