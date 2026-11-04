package com.fdossena.speedtest.core.upload;

import java.io.OutputStream;
import java.util.Random;

import com.fdossena.speedtest.core.base.Connection;

public abstract class Uploader extends Thread{
    private Connection c;
    private String path;
    private volatile boolean stopASAP=false, resetASAP=false;
    private volatile long totUploaded=0;
    private byte[] garbage;

    private static byte[] sharedGarbage;
    //the payload is random so it cannot be compressed on the way; one copy per
    //process is enough -- filling 20 MB per stream and per restart stalled the
    //start of the upload phase and spiked the heap
    private static synchronized byte[] garbage(int ckSize){
        int size=ckSize*1048576;
        if(sharedGarbage==null||sharedGarbage.length!=size){
            byte[] g=new byte[size];
            new Random(System.nanoTime()).nextBytes(g);
            sharedGarbage=g;
        }
        return sharedGarbage;
    }

    public Uploader(Connection c, String path, int ckSize){
        this.c=c;
        this.path=path;
        garbage=garbage(ckSize);
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
            //a stopped stream closes the connection to unblock this thread;
            //that is a clean stop, not an error
            if(!stopASAP) onError(t.toString());
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
