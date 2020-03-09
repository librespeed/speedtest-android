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
                boolean chunked=false;
                boolean ok=false;
                while(true){
                    String l=c.readLineUnbuffered();
                    if(l==null) break;
                    l=l.trim().toLowerCase();
                    if(l.equals("transfer-encoding: chunked")) chunked=true;
                    if(l.contains("200 ok")) ok=true;
                    if(l.trim().isEmpty()){
                        if(chunked){c.readLineUnbuffered(); c.readLineUnbuffered();}
                        break;
                    }
                }
                if(!ok) throw new Exception("Did not get a 200");
                t=System.nanoTime()-t;
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