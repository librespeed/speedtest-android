package com.fdossena.speedtest.core.download;

import java.io.InputStream;

import com.fdossena.speedtest.core.base.Connection;
import com.fdossena.speedtest.core.base.Utils;

public abstract class Downloader extends Thread{
    private Connection c;
    private String path;
    private int ckSize;
    private boolean stopASAP=false, resetASAP=false;
    private long totDownloaded=0;

    public Downloader(Connection c, String path, int ckSize){
        this.c=c;
        this.path=path;
        this.ckSize=ckSize<1?1:ckSize;
        start();
    }

    private static final int BUFFER_SIZE=16384;
    public void run(){
        try{
            String s=path;
            s+= Utils.url_sep(s)+"ckSize="+ckSize;
            long lastProgressEvent=System.currentTimeMillis();
            long ckBytes=ckSize*1048576, newRequestThreshold=ckBytes/4;
            long bytesLeft=0;
            InputStream in=c.getInputStream();
            byte[] buf=new byte[BUFFER_SIZE];
            for(;;){
                if(stopASAP) break;
                if(bytesLeft<=newRequestThreshold){
                    c.GET(s, true);
                    bytesLeft+=ckBytes;
                }
                if(stopASAP) break;
                int l=in.read(buf);
                if(stopASAP) break;
                bytesLeft-=l;
                if(resetASAP){
                    totDownloaded=0;
                    resetASAP=false;
                }
                totDownloaded+=l;
                if(System.currentTimeMillis()-lastProgressEvent>200){
                    lastProgressEvent=System.currentTimeMillis();
                    onProgress(totDownloaded);
                }
            }
            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    public void stopASAP(){
        this.stopASAP=true;
    }

    public abstract void onProgress(long downloaded);
    public abstract void onError(String err);

    public void resetDownloadCounter(){
        resetASAP=true;
    }

    public long getDownloaded() {
        return resetASAP?0:totDownloaded;
    }
}