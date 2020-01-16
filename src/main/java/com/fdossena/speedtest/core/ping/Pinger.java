package com.fdossena.speedtest.core.ping;

import java.io.InputStream;

import com.fdossena.speedtest.core.base.Connection;

public abstract class Pinger extends Thread{
    private Connection c;
    private String path;
    private boolean stopASAP=false;

    public Pinger(Connection c, String path){
        this.c=c;
        this.path=path;
        start();
    }

    public void run(){
        try{
            String s=path;
            InputStream in=c.getInputStream();
            for(;;){
                if(stopASAP) break;
                c.GET(s,true);
                if(stopASAP) break;
                long t=System.nanoTime();
                if(c.readLineUnbuffered().trim().isEmpty()) throw new Exception("Persistent connection died");
                t=System.nanoTime()-t;
                if(stopASAP) break;
                while(!c.readLineUnbuffered().trim().isEmpty());
                if(stopASAP) break;
                if(!onPong(t/2)) break;
            }
            c.close();
        }catch(Throwable t){
            try{c.close();}catch(Throwable t1){}
            onError(t.toString());
        }
    }

    public abstract boolean onPong(long ns);
    public abstract void onError(String err);

    public void stopASAP(){
        this.stopASAP=true;
    }
}