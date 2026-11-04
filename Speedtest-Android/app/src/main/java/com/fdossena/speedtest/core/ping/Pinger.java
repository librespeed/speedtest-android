package com.fdossena.speedtest.core.ping;

import java.io.InputStream;

import com.fdossena.speedtest.core.base.Connection;

public abstract class Pinger extends Thread{
    private Connection c;
    private String path;
    //written by the caller's thread, read by the ping thread: the bufferbloat
    //pinger runs bare, without a PingStream to close its connection underneath
    private volatile boolean stopASAP=false;

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
                    if(l.startsWith("http/")){
                        String[] statusParts=l.split(" ");
                        if(statusParts.length>=2&&statusParts[1].startsWith("2")) ok=true;
                    }
                    if(l.trim().isEmpty()){
                        if(chunked){c.readLineUnbuffered(); c.readLineUnbuffered();}
                        break;
                    }
                }
                if(!ok) throw new Exception("Did not get a 200");
                t=System.nanoTime()-t;
                if(stopASAP) break;
                //the full request-response time, as the web client and the CLI report it;
                //halving it used to cancel out a round trip that Nagle's algorithm added
                if(!onPong(t)) break;
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