package com.fdossena.speedtest.core.upload;

import java.io.OutputStream;
import java.util.Random;

import com.fdossena.speedtest.core.base.Connection;

public abstract class Uploader extends Thread{
    private Connection c;
    private String path;
    private boolean stopASAP=false, resetASAP=false;
    private long totUploaded=0;
    private byte[] garbage;

    public Uploader(Connection c, String path, int ckSize){
        this.c=c;
        this.path=path;
        garbage=new byte[ckSize*1048576];
        Random r=new Random(System.nanoTime());
        r.nextBytes(garbage);
        start();
    }

    private static final int BUFFER_SIZE=16384;
    public void run(){
        try{
            String s=path;
            long lastProgressEvent=System.currentTimeMillis();
            OutputStream out=c.getOutputStream();
            byte[] buf=new byte[BUFFER_SIZE];
            for(;;){
                if(stopASAP) break;
                c.POST(s,true,"application/octet-stream",garbage.length);
                for(int offset=0;offset<garbage.length;offset+=BUFFER_SIZE){
                    if(stopASAP) break;
                    int l=(offset+BUFFER_SIZE>=garbage.length)?(garbage.length-offset):BUFFER_SIZE;
                    out.write(garbage,offset,l);
                    if(stopASAP) break;
                    if(resetASAP){
                        totUploaded=0;
                        resetASAP=false;
                    }
                    totUploaded+=l;
                    if(System.currentTimeMillis()-lastProgressEvent>200){
                        lastProgressEvent=System.currentTimeMillis();
                        onProgress(totUploaded);
                    }
                }
                if(stopASAP) break;
                while(!c.readLineUnbuffered().trim().isEmpty());
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

    public abstract void onProgress(long uploaded);
    public abstract void onError(String err);

    public void resetUploadCounter(){
        resetASAP=true;
    }

    public long getUploaded() {
        return resetASAP?0:totUploaded;
    }
}
