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
        super("DownLoader");
        this.c=c;
        this.path=path;
        this.ckSize=ckSize<1?1:ckSize;
        start();
    }

    private static final int BUFFER_SIZE=16384;
    public void run(){
        InputStream in = null;
        try{
            String s=path;
            s+= Utils.url_sep(s)+"ckSize="+ckSize;
            long lastProgressEvent=System.currentTimeMillis();
            long ckBytes=ckSize*1048576, newRequestThreshold=ckBytes/4;
            long bytesLeft=0;
            in=c.getInputStream();
            byte[] buf=new byte[BUFFER_SIZE];
            for(;;){
                synchronized(this)  { if(stopASAP) break; }
                if(bytesLeft<=newRequestThreshold){
                    c.GET(s, true);
                    bytesLeft+=ckBytes;
                }
                synchronized(this)  { if(stopASAP) break; }
                int l=in.read(buf);
                long curTotDownloaded;
                synchronized(this) {
                    if (stopASAP) break;
                    bytesLeft -= l;
                    if (resetASAP) {
                        totDownloaded = 0;
                        resetASAP = false;
                    }
                    totDownloaded+=l;
                    curTotDownloaded = totDownloaded;
                }

                if(System.currentTimeMillis()-lastProgressEvent>200){
                    lastProgressEvent=System.currentTimeMillis();
                    onProgress(curTotDownloaded); // makes the call outside the critical region using a local variable as parameter
                }
            }
        }catch(Throwable t){
            onError(t.toString());
        }
        finally {
            try { if (in!=null) in.close(); } catch(Throwable t1){}
            try{c.close();}catch(Throwable t1){}
            onEnd();
        }
    }

    public abstract void onEnd();
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