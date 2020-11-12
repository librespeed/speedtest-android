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
        super("Uploader");
        this.c=c;
        this.path=path;
        garbage=new byte[ckSize*1048576];
        Random r=new Random(System.nanoTime());
        long time1 = System.currentTimeMillis();
        r.nextBytes(garbage);
        long time2 = System.currentTimeMillis();
        System.out.println("Random byte generation took "+(time2-time1)+" ms for "+garbage.length+" bytes");
        start();
    }
/*
    public void run()
    {
        onEnd();
    }
*/
    private static final int BUFFER_SIZE=16384;
    public void run(){
        System.out.println("Uploader run");
        OutputStream out = null;
        try
        {
            String s=path;
            long lastProgressEvent=System.currentTimeMillis();
            out=c.getOutputStream();
            //byte[] buf=new byte[BUFFER_SIZE];
            int n = 0;
            for(;;){
                synchronized(this)  { if(stopASAP) break; }
                System.out.println("Uploader loop");
                c.POST(s,true,"application/octet-stream",garbage.length);
                for(int offset=0;offset<garbage.length;offset+=BUFFER_SIZE){
                    synchronized(this)  { if(stopASAP) break; }
                    int l=(offset+BUFFER_SIZE>=garbage.length)?(garbage.length-offset):BUFFER_SIZE;
                    //System.out.println("Uploader before out");
                    out.write(garbage,offset,l);
                    //System.out.println("Uploader after out");
                    long curTotUploaded;
                    synchronized(this)  {
                        if(stopASAP) break;
                        if(resetASAP) {
                            totUploaded = 0;
                            resetASAP = false;
                        }
                        totUploaded+=l;
                        curTotUploaded = totUploaded;
                    }
                    if(System.currentTimeMillis()-lastProgressEvent>200){
                        lastProgressEvent=System.currentTimeMillis();
                        System.out.println("Uploader before on progress");
                        onProgress(curTotUploaded); // makes the call outside the critical region using a local variable as parameter
                    }
                    //System.out.println("Loop "+n+" "+offset);
                    n++;
                }
                synchronized(this)  { if(stopASAP) break; }
                for(;;)
                {
                    String lin = c.readLineUnbuffered();
                    System.out.println(lin);
                    if (lin==null || lin.trim().isEmpty())
                        break;
                }
            }
        } catch(Throwable t){
            t.printStackTrace();
            onError(t.toString());
        } finally {
            try {
                if (out!=null) out.close();
            }
            catch(Throwable t1) {
                t1.printStackTrace();
            }
            try {
                c.close();
            } catch (Throwable t1)
            {
                t1.printStackTrace();
            }
            onEnd();
        }
    }

    public synchronized void stopASAP(){
        this.stopASAP=true;
    }

    public abstract void onEnd();
    public abstract void onProgress(long uploaded);
    public abstract void onError(String err);
    public abstract void onWarning(String err);

    public synchronized void resetUploadCounter(){
        resetASAP=true;
    }
    public synchronized long getUploaded() {
        return resetASAP?0:totUploaded;
    }
}
